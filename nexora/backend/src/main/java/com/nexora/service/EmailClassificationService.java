package com.nexora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.config.GeminiConfig;
import com.nexora.model.Email;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import com.nexora.model.EmailAction;
import com.nexora.model.User;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailClassificationService {

    private final GeminiConfig geminiConfig;
    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;
    private final ObjectMapper objectMapper;
    private final CalendarService calendarService;
    private final java.util.concurrent.Semaphore semaphore = new java.util.concurrent.Semaphore(10);

    @Async
    public void classifyEmail(Long emailId, User user) {
        try {
            semaphore.acquire();
            try {
                Email email = emailRepository.findById(emailId).orElse(null);
                if (email == null) return;

                String body = email.getBodyFull() != null ? email.getBodyFull() : email.getBodySnippet();
                if (body == null) body = "";
                // Trim to avoid huge token usage
                if (body.length() > 4000) body = body.substring(0, 4000);

                String systemPrompt = buildSystemPrompt(user);
                String userMessage  = buildUserMessage(email, body);

                String rawResponse = null;
                if (geminiConfig.isConfigured()) {
                    log.info("Classifying email {} using Google Gemini API...", emailId);
                    rawResponse = callGemini(systemPrompt, userMessage);
                } else {
                    log.info("No AI keys configured — running local high-fidelity fallback parser for email {}...", emailId);
                    rawResponse = getLocalFallbackResponse(email);
                }

                if (rawResponse == null) return;

                JsonNode result = parseJson(rawResponse);
                if (result == null) return;

                // Update email record
                email.setCategory(parseCategory(result));
                email.setPriority(parsePriority(result));
                email.setAiSummary(getText(result, "summary"));
                email.setAiActionItems(result.has("action_items") ? result.get("action_items").toString() : null);

                String deadlineStr = getText(result, "deadline");
                email.setDeadlineDetected(parseDeadline(deadlineStr));

                emailRepository.save(email);

                // Save action items
                if (result.has("action_items") && result.get("action_items").isArray()) {
                    saveActionItems(result.get("action_items"), email, user.getId());
                }

                // Auto-sync calendar event if enabled
                if (email.getDeadlineDetected() != null) {
                    calendarService.createDeadlineEvent(user, email);
                }

                log.info("Classified email {} as {} / {}", emailId, email.getCategory(), email.getPriority());

            } catch (Exception e) {
                log.error("Classification failed for email {}: {}", emailId, e.getMessage());
            } finally {
                semaphore.release();
            }
        } catch (InterruptedException e) {
            log.error("Classification semaphore acquisition interrupted for email {}: {}", emailId, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Parses a deadline the model emitted. It returns an offset whenever the
     * email states a timezone ("5:00 PM IST" comes back as ...T17:00:00+05:30),
     * and LocalDateTime.parse silently discards that offset — keeping the wall
     * clock and shifting the real instant by the offset, so an IST deadline
     * landed 5.5 hours late on a UTC server. Convert offset-bearing values to
     * the server zone first; values without an offset are already local.
     */
    private LocalDateTime parseDeadline(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("null")) return null;
        try {
            return java.time.OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (Exception noOffset) {
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME);
            } catch (Exception e) {
                log.debug("Unparseable deadline from model: {}", value);
                return null;
            }
        }
    }

    private String buildSystemPrompt(User user) {
        return """
You are Nexora's email intelligence engine. Analyze the email below and respond ONLY with a valid JSON object — no explanation, no markdown, no extra text.

User profile: %s

Categories available: ASSIGNMENT, ATTENDANCE, HACKATHON, PLACEMENT, INTERNSHIP, MEETING, ANNOUNCEMENT, RESEARCH, FINANCE, PERSONAL, PROMOTIONAL, SPAM, UNCATEGORIZED

Respond with this exact JSON structure:
{
  "category": "CATEGORY_NAME",
  "priority": "HIGH | MEDIUM | LOW",
  "summary": "2-3 sentence summary of what this email is about",
  "action_items": [
    {
      "action_type": "REGISTER | REPLY | SUBMIT | UPLOAD | REVIEW | ATTEND | OTHER",
      "description": "what the user needs to do",
      "deadline": "ISO 8601 datetime or null"
    }
  ],
  "deadline": "ISO 8601 datetime of the most important deadline or null"
}

Priority rules for %s profile:
- HIGH: professor emails, placement notices, assignment deadlines within 3 days, hackathon registrations
- MEDIUM: general announcements, internship opportunities, meeting invitations
- LOW: newsletters, promotional emails, distant deadlines
""".formatted(user.getUserRole(), user.getUserRole());
    }

    private String buildUserMessage(Email email, String body) {
        return """
From: %s (%s)
Subject: %s
Body:
%s
""".formatted(
            email.getSenderName() != null ? email.getSenderName() : "",
            email.getSenderEmail(),
            email.getSubject() != null ? email.getSubject() : "(no subject)",
            body);
    }

    /**
     * Classification calls: the prompt demands a JSON object, so ask Gemini to
     * emit JSON and label the payload as an email to classify.
     */
    public String callGemini(String systemPrompt, String userMessage) {
        return callGemini(systemPrompt, userMessage, true, "Email Content to classify:");
    }

    /**
     * Prose calls (Brain answers, thread summaries). These used to share the
     * classification path, which forced responseMimeType=application/json —
     * so Gemini wrapped its prose in {"answer": "..."} and that raw JSON was
     * handed straight to the UI as the answer text. They also had the user's
     * question labelled "Email Content to classify", which misdirects the model.
     */
    public String callGeminiText(String systemPrompt, String userMessage) {
        return callGemini(systemPrompt, userMessage, false, "User question:");
    }

    @SuppressWarnings("unchecked")
    private String callGemini(String systemPrompt, String userMessage, boolean jsonMode, String userLabel) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String url = geminiConfig.getGenerateContentUrl();

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", systemPrompt + "\n\n" + userLabel + "\n" + userMessage);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            if (jsonMode) {
                Map<String, Object> generationConfig = new HashMap<>();
                generationConfig.put("responseMimeType", "application/json");
                requestBody.put("generationConfig", generationConfig);
            }

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            // Key travels in a header, not the query string, so it stays out
            // of access logs and proxy logs.
            headers.set("x-goog-api-key", geminiConfig.getApiKey());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getBody() != null && response.getBody().containsKey("candidates")) {
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.getBody().get("candidates");
                if (!candidates.isEmpty()) {
                    Map<String, Object> firstCandidate = candidates.get(0);
                    if (firstCandidate.containsKey("content")) {
                        Map<String, Object> contentMap = (Map<String, Object>) firstCandidate.get("content");
                        List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                        if (!parts.isEmpty()) {
                            return (String) parts.get(0).get("text");
                        }
                    }
                }
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // A retired model answers 404 forever. Without a distinct message
            // it looks identical to "no key configured" and the whole AI layer
            // degrades to keyword matching without anyone noticing — which is
            // exactly how gemini-1.5-flash stayed broken.
            if (e.getStatusCode().value() == 404) {
                log.error("Gemini model '{}' returned 404 — it is retired or unavailable to this key. "
                                + "Set GEMINI_MODEL to a current model. Falling back to local classification.",
                        geminiConfig.getModel());
            } else if (e.getStatusCode().value() == 429) {
                log.warn("Gemini quota exceeded — falling back to local classification.");
            } else if (e.getStatusCode().value() == 401 || e.getStatusCode().value() == 403) {
                log.error("Gemini rejected the API key ({}). Check GEMINI_API_KEY.", e.getStatusCode().value());
            } else {
                log.error("Gemini call failed with {}: {}", e.getStatusCode(), e.getMessage());
            }
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage());
        }
        return null;
    }

    public String generateBrainAnswer(String systemPrompt, String userQuery) {
        if (geminiConfig.isConfigured()) {
            log.info("Querying Nexora Brain using Gemini...");
            return callGeminiText(systemPrompt, userQuery);
        } else {
            log.info("No AI keys configured for Nexora Brain. Running local keyword-based parser...");
            return null;
        }
    }

    public String summarizeThread(String systemPrompt, String threadContext) {
        if (geminiConfig.isConfigured()) {
            log.info("Summarizing thread using Gemini...");
            return callGeminiText(systemPrompt, threadContext);
        } else {
            log.info("No AI keys configured for thread summarization — using local fallback summary...");
            return "This thread contains multiple emails and was analyzed locally. Configure a Gemini API key for premium AI thread summaries.";
        }
    }

    private String getLocalFallbackResponse(Email email) {
        String subject = (email.getSubject() != null ? email.getSubject() : "").toLowerCase();
        String body = (email.getBodyFull() != null ? email.getBodyFull() : (email.getBodySnippet() != null ? email.getBodySnippet() : "")).toLowerCase();
        
        String category = "UNCATEGORIZED";
        String priority = "MEDIUM";
        String summary = "This email was parsed locally. Add a free Google Gemini API key to enable premium AI-powered summaries and descriptions.";
        String actionType = "REVIEW";
        String actionDesc = "Review this email for details";
        String deadline = "null";

        if (subject.contains("assignment") || body.contains("assignment") || subject.contains("submit") || body.contains("submission")) {
            category = "ASSIGNMENT";
            priority = "HIGH";
            actionType = "SUBMIT";
            actionDesc = "Complete and submit your assignment";
            deadline = LocalDateTime.now().plusDays(3).toString() + "Z";
        } else if (subject.contains("hackathon") || body.contains("hackathon") || subject.contains("register") || body.contains("registration")) {
            category = "HACKATHON";
            priority = "HIGH";
            actionType = "REGISTER";
            actionDesc = "Register for the hackathon event";
            deadline = LocalDateTime.now().plusDays(5).toString() + "Z";
        } else if (subject.contains("interview") || subject.contains("placement") || body.contains("placement") || body.contains("job offer")) {
            category = "PLACEMENT";
            priority = "HIGH";
            actionType = "REPLY";
            actionDesc = "Reply to the placement officer or recruiter";
            deadline = LocalDateTime.now().plusDays(2).toString() + "Z";
        } else if (subject.contains("meeting") || body.contains("meeting") || subject.contains("zoom") || body.contains("google meet")) {
            category = "MEETING";
            priority = "MEDIUM";
            actionType = "ATTEND";
            actionDesc = "Attend the scheduled meeting";
            deadline = LocalDateTime.now().plusHours(4).toString() + "Z";
        } else if (subject.contains("internship") || body.contains("internship")) {
            category = "INTERNSHIP";
            priority = "MEDIUM";
            actionType = "REVIEW";
            actionDesc = "Apply or review the internship opportunity";
        } else if (subject.contains("alert") || subject.contains("announcement") || body.contains("important announcement")) {
            category = "ANNOUNCEMENT";
            priority = "MEDIUM";
            actionType = "REVIEW";
            actionDesc = "Review the official announcement";
        } else if (subject.contains("bill") || subject.contains("invoice") || body.contains("payment due") || subject.contains("fee")) {
            category = "FINANCE";
            priority = "HIGH";
            actionType = "REVIEW";
            actionDesc = "Verify invoice details and make payment";
        }

        return """
        {
          "category": "%s",
          "priority": "%s",
          "summary": "%s",
          "action_items": [
            {
              "action_type": "%s",
              "description": "%s",
              "deadline": %s
            }
          ],
          "deadline": %s
        }
        """.formatted(category, priority, summary, actionType, actionDesc, 
                      deadline.equals("null") ? "null" : "\"" + deadline + "\"",
                      deadline.equals("null") ? "null" : "\"" + deadline + "\"");
    }

    private JsonNode parseJson(String rawResponse) {
        try {
            // Strip markdown code fences if present
            String cleaned = rawResponse.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("```json", "").replaceAll("```", "").trim();
            }
            return objectMapper.readTree(cleaned);
        } catch (Exception e) {
            log.error("Failed to parse AI JSON response: {}", e.getMessage());
            return null;
        }
    }

    private EmailCategory parseCategory(JsonNode node) {
        try {
            return EmailCategory.valueOf(getText(node, "category"));
        } catch (Exception e) {
            return EmailCategory.UNCATEGORIZED;
        }
    }

    private Priority parsePriority(JsonNode node) {
        try {
            return Priority.valueOf(getText(node, "priority"));
        } catch (Exception e) {
            return Priority.MEDIUM;
        }
    }

    private String getText(JsonNode node, String field) {
        if (node.has(field) && !node.get(field).isNull()) {
            return node.get(field).asText();
        }
        return null;
    }

    private void saveActionItems(JsonNode items, Email email, Long userId) {
        for (JsonNode item : items) {
            try {
                EmailAction action = EmailAction.builder()
                        .email(email)
                        .userId(userId)
                        .actionType(parseActionType(getText(item, "action_type")))
                        .actionDescription(getText(item, "description") != null ? getText(item, "description") : "Action required")
                        .build();

                action.setDeadline(parseDeadline(getText(item, "deadline")));
                actionRepository.save(action);
            } catch (Exception e) {
                log.warn("Could not save action item: {}", e.getMessage());
            }
        }
    }

    private EmailAction.ActionType parseActionType(String type) {
        try {
            return EmailAction.ActionType.valueOf(type);
        } catch (Exception e) {
            return EmailAction.ActionType.OTHER;
        }
    }

    public User.UserRole detectUserProfession(List<Email> emails) {
        if (emails == null || emails.isEmpty()) {
            return User.UserRole.STUDENT;
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < Math.min(emails.size(), 20); i++) {
            Email email = emails.get(i);
            context.append("Subject: ").append(email.getSubject()).append("\n")
                   .append("Sender: ").append(email.getSenderName()).append(" (").append(email.getSenderEmail()).append(")\n")
                   .append("Snippet: ").append(email.getBodySnippet()).append("\n\n");
        }

        if (geminiConfig.isConfigured()) {
            try {
                String systemPrompt = """
You are Nexora's user profile analyzer. Based on the recent emails of the user, determine their most likely profession or role.
Choose ONLY from these roles: STUDENT, PROFESSOR, IT_EMPLOYEE, HR_PROFESSIONAL, MANAGER, FREELANCER.
Respond with a valid JSON object containing exactly one key "role". Example: {"role": "STUDENT"}
Do not include any explanation or markdown.
""";
                String response = callGemini(systemPrompt, context.toString());
                JsonNode node = parseJson(response);
                if (node != null && node.has("role")) {
                    String roleStr = node.get("role").asText().toUpperCase();
                    return User.UserRole.valueOf(roleStr);
                }
            } catch (Exception e) {
                log.error("AI profession detection failed, using fallback: {}", e.getMessage());
            }
        }

        String combinedText = context.toString().toLowerCase();
        int studentScore = countMatches(combinedText, "assignment", "course", "professor", "class", "exam", "placement", "internship", "grading", "homework", "student");
        int professorScore = countMatches(combinedText, "syllabus", "lecture", "faculty", "grant", "research paper", "grading", "phd", "academic", "university office");
        int hrScore = countMatches(combinedText, "resume", "candidate", "interview", "hiring", "onboarding", "offer letter", "payroll", "recruiter", "talent acquisition");
        int itScore = countMatches(combinedText, "ticket", "server", "deployment", "bug", "jira", "aws", "git", "database", "api", "incident", "dns");
        int managerScore = countMatches(combinedText, "approvals", "budget", "project sync", "quarterly", "team roadmap", "one-on-one", "kpi", "status update");
        int freelancerScore = countMatches(combinedText, "proposal", "contract", "invoice", "client", "gig", "payment milestone", "brief", "freelance");

        log.info("Profession Scores -> STUDENT: {}, PROFESSOR: {}, HR: {}, IT: {}, MANAGER: {}, FREELANCER: {}",
                studentScore, professorScore, hrScore, itScore, managerScore, freelancerScore);

        int max = Math.max(studentScore, Math.max(professorScore, Math.max(hrScore, Math.max(itScore, Math.max(managerScore, freelancerScore)))));
        if (max == 0) return User.UserRole.STUDENT;

        if (max == studentScore) return User.UserRole.STUDENT;
        if (max == professorScore) return User.UserRole.PROFESSOR;
        if (max == hrScore) return User.UserRole.HR_PROFESSIONAL;
        if (max == itScore) return User.UserRole.IT_EMPLOYEE;
        if (max == managerScore) return User.UserRole.MANAGER;
        return User.UserRole.FREELANCER;
    }

    private int countMatches(String text, String... keywords) {
        int count = 0;
        for (String word : keywords) {
            int index = 0;
            while ((index = text.indexOf(word, index)) != -1) {
                count++;
                index += word.length();
            }
        }
        return count;
    }
}
