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
import com.nexora.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public class EmailClassificationService {

    /** Controls when Gemini is invoked — batch stays local; enrichment is selective. */
    private enum GeminiUse {
        /** Fast local + Gmail fallback only (sync / batch). */
        NONE,
        /** Gemini only for important or ambiguous mail (background pass). */
        SELECTIVE,
        /** User opened a message — always try Gemini when configured. */
        ALWAYS
    }

    private final GeminiConfig geminiConfig;
    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final EmailActionRepository actionRepository;
    private final ObjectMapper objectMapper;
    private final CalendarService calendarService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final java.util.concurrent.Semaphore geminiSemaphore;

    public EmailClassificationService(GeminiConfig geminiConfig,
                                      EmailRepository emailRepository,
                                      UserRepository userRepository,
                                      EmailActionRepository actionRepository,
                                      ObjectMapper objectMapper,
                                      CalendarService calendarService) {
        this.geminiConfig = geminiConfig;
        this.emailRepository = emailRepository;
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.objectMapper = objectMapper;
        this.calendarService = calendarService;
        this.geminiSemaphore = new java.util.concurrent.Semaphore(geminiConfig.getMaxConcurrent());
    }

    @Async
    public void classifyEmail(Long emailId, User user) {
        try {
            geminiSemaphore.acquire();
            try {
                Email email = emailRepository.findById(emailId).orElse(null);
                if (email == null) return;
                classifyAndPersist(email, user, GeminiUse.ALWAYS);
                log.info("Classified email {} as {} / {}", emailId, email.getCategory(), email.getPriority());
            } catch (Exception e) {
                log.error("Classification failed for email {}: {}", emailId, e.getMessage());
            } finally {
                geminiSemaphore.release();
            }
        } catch (InterruptedException e) {
            log.error("Classification semaphore acquisition interrupted for email {}: {}", emailId, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    private String buildSystemPrompt(User user) {
        return RoleClassificationProfile.buildSystemPrompt(
                user.getUserRole(),
                user.getEmail());
    }

    private String buildUserMessage(Email email, String body) {
        String labels = email.getGmailLabelIds() != null ? email.getGmailLabelIds() : "[]";
        return """
From: %s (%s)
Subject: %s
Gmail labels: %s
Body:
%s
""".formatted(
            email.getSenderName() != null ? email.getSenderName() : "",
            email.getSenderEmail(),
            email.getSubject() != null ? email.getSubject() : "(no subject)",
            labels,
            body);
    }

    /**
     * Core Gemini HTTP call. {@code contentLabel} names the user payload section.
     * Use {@code jsonResponse=true} for classification / structured outputs only.
     */
    @SuppressWarnings("unchecked")
    public String callGemini(String systemPrompt, String userMessage, String contentLabel, boolean jsonResponse) {
        if (!geminiConfig.isConfigured()) {
            return null;
        }
        try {
            String url = geminiConfig.getGenerateContentUrl();

            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> part = new HashMap<>();
            part.put("text", systemPrompt + "\n\n" + contentLabel + ":\n" + userMessage);
            Map<String, Object> content = new HashMap<>();
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            Map<String, Object> generationConfig = new HashMap<>();
            if (jsonResponse) {
                generationConfig.put("responseMimeType", "application/json");
                generationConfig.put("temperature", 0.2);
            } else {
                generationConfig.put("temperature", 0.4);
            }
            requestBody.put("generationConfig", generationConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
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
            return callGemini(systemPrompt, userQuery, "User query", false);
        }
        log.info("No AI keys configured for Nexora Brain. Running local keyword-based parser...");
        return null;
    }

    public String summarizeThread(String systemPrompt, String threadContext) {
        if (geminiConfig.isConfigured()) {
            log.info("Summarizing thread using Gemini...");
            return callGemini(systemPrompt, threadContext, "Thread messages", false);
        }
        log.info("No AI keys configured for thread summarization — using local fallback summary...");
        return "This thread contains multiple emails and was analyzed locally. Configure a Gemini API key for premium AI thread summaries.";
    }

    /**
     * Synchronous group-by-source-and-content. Used after inbox sync so mail
     * appears first, then is separated into groups. Does not invent data.
     * Caps work per call so classify does not load the entire mailbox into memory.
     */
    @org.springframework.transaction.annotation.Transactional
    public int classifyInboxBySourceAndContent(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Cannot classify inbox — user {} not found", userId);
            return 0;
        }

        recategorizeMarketingMislabels(userId);

        final int batchSize = 250;
        int totalClassified = 0;
        for (int pass = 0; pass < 30; pass++) {
            List<Email> toClassify = emailRepository.findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
                    userId,
                    EmailCategory.UNCATEGORIZED,
                    org.springframework.data.domain.PageRequest.of(0, batchSize)
            ).getContent();
            if (toClassify.isEmpty()) {
                break;
            }
            if (pass == 0) {
                Set<Long> seen = new HashSet<>();
                for (Email archived : emailRepository.findByUserIdAndIsArchivedTrueOrderByReceivedAtDesc(
                        userId, org.springframework.data.domain.PageRequest.of(0, 100)).getContent()) {
                    if (archived.getId() != null && seen.add(archived.getId())
                            && (archived.getCategory() == null || archived.getCategory() == EmailCategory.UNCATEGORIZED)
                            && !Boolean.TRUE.equals(archived.getIsDraft())) {
                        toClassify.add(archived);
                    }
                }
            }

            int classifiedThisPass = 0;
            Set<Long> seen = new HashSet<>();
            for (Email email : toClassify) {
                if (email.getId() != null && !seen.add(email.getId())) continue;
                if (Boolean.TRUE.equals(email.getIsDraft())) continue;
                if (email.getCategory() != null && email.getCategory() != EmailCategory.UNCATEGORIZED) {
                    continue;
                }
                try {
                    if (applySourceContentClassification(email, user)) {
                        classifiedThisPass++;
                    }
                } catch (Exception e) {
                    log.warn("Local classify failed for email {}: {}", email.getId(), e.getMessage());
                }
            }
            totalClassified += classifiedThisPass;
            long remaining = emailRepository.countByUserIdAndCategoryAndInInboxTrue(
                    userId, EmailCategory.UNCATEGORIZED);
            if (remaining == 0 || classifiedThisPass == 0) {
                break;
            }
        }

        // Belt-and-suspenders: Gmail-grounded categories for any stragglers
        while (true) {
            List<Email> stragglers = emailRepository.findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
                    userId,
                    EmailCategory.UNCATEGORIZED,
                    org.springframework.data.domain.PageRequest.of(0, batchSize)
            ).getContent();
            if (stragglers.isEmpty()) {
                break;
            }
            int swept = 0;
            for (Email email : stragglers) {
                if (Boolean.TRUE.equals(email.getIsDraft())) continue;
                try {
                    classifyAndPersist(email, user, GeminiUse.NONE);
                    if (email.getCategory() != EmailCategory.UNCATEGORIZED) {
                        totalClassified++;
                        swept++;
                    }
                } catch (Exception e) {
                    log.warn("Straggler classify failed for email {}: {}", email.getId(), e.getMessage());
                }
            }
            if (swept == 0) {
                break;
            }
        }

        long left = emailRepository.countByUserIdAndCategoryAndInInboxTrue(userId, EmailCategory.UNCATEGORIZED);
        if (left > 0) {
            log.warn("Inbox classify finished with {} uncategorized inbox messages for user {}", left, userId);
        } else if (geminiConfig.isConfigured()) {
            refineInboxWithGeminiAsync(userId);
        }
        return totalClassified;
    }

    /**
     * Background Gemini enrichment for high-value mail after fast local classification.
     * Capped per run so quota stays predictable; skips promos/spam and mail with good summaries.
     */
    @Async
    public void refineInboxWithGeminiAsync(Long userId) {
        if (!geminiConfig.isConfigured()) {
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return;
        }

        int budget = geminiConfig.getRefinementBatchSize();
        List<Email> candidates = collectGeminiRefinementCandidates(userId, user, budget);
        if (candidates.isEmpty()) {
            log.debug("No Gemini refinement candidates for user {}", userId);
            return;
        }

        log.info("Gemini background refinement for user {} — {} candidates (budget {})",
                userId, candidates.size(), budget);
        int refined = 0;
        for (Email email : candidates) {
            try {
                geminiSemaphore.acquire();
                try {
                    classifyAndPersist(email, user, GeminiUse.SELECTIVE);
                    refined++;
                } finally {
                    geminiSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.warn("Gemini refinement failed for email {}: {}", email.getId(), e.getMessage());
            }
        }
        log.info("Gemini background refinement finished for user {} — {} emails enriched", userId, refined);
    }

    private List<Email> collectGeminiRefinementCandidates(Long userId, User user, int limit) {
        User.UserRole role = user.getUserRole() != null ? user.getUserRole() : User.UserRole.STUDENT;
        Set<Long> seen = new HashSet<>();
        List<Email> out = new ArrayList<>();

        addGeminiCandidates(out, seen, emailRepository.findGeminiPriorityCandidates(
                userId, org.springframework.data.domain.PageRequest.of(0, limit)).getContent(), role, limit);

        if (out.size() < limit) {
            addGeminiCandidates(out, seen, emailRepository.findByUserIdAndInInboxTrueOrderByReceivedAtDesc(
                    userId, org.springframework.data.domain.PageRequest.of(0, limit * 2)).getContent(), role, limit);
        }
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    private void addGeminiCandidates(List<Email> out, Set<Long> seen, List<Email> source,
                                     User.UserRole role, int limit) {
        for (Email email : source) {
            if (out.size() >= limit) break;
            if (email.getId() == null || !seen.add(email.getId())) continue;
            if (Boolean.TRUE.equals(email.getIsDraft())) continue;
            if (email.getCategory() == EmailCategory.UNCATEGORIZED) continue;

            String category = email.getCategory() != null ? email.getCategory().name() : "UNCATEGORIZED";
            if (!RoleClassificationProfile.worthGeminiRefinement(
                    category, email.getAiSummary(), email, role)) {
                continue;
            }
            out.add(email);
        }
    }

    /**
     * Re-runs classification for every inbox message using the user's current account preferences.
     */
    @org.springframework.transaction.annotation.Transactional
    public int reclassifyInboxForPreferences(Long userId) {
        int reset = emailRepository.resetInboxCategoriesExceptSpam(userId);
        log.info("Reset {} inbox emails for preference-based reclassification (user {})", reset, userId);
        recategorizeMarketingMislabels(userId);
        return classifyInboxBySourceAndContent(userId);
    }

    @Async
    public void reclassifyInboxForPreferencesAsync(Long userId) {
        try {
            int classified = reclassifyInboxForPreferences(userId);
            log.info("Async preference reclassification finished for user {} — {} emails processed", userId, classified);
        } catch (Exception e) {
            log.error("Async preference reclassification failed for user {}: {}", userId, e.getMessage());
        }
    }

    private boolean applySourceContentClassification(Email email, User user) {
        classifyAndPersist(email, user, GeminiUse.NONE);
        return email.getCategory() != null && email.getCategory() != EmailCategory.UNCATEGORIZED;
    }

    private void classifyAndPersist(Email email, User user, GeminiUse geminiUse) {
        User.UserRole role = user.getUserRole() != null ? user.getUserRole() : User.UserRole.STUDENT;
        String body = emailBodyForClassification(email);
        JsonNode result = parseJson(getLocalFallbackResponse(email, role));

        if (geminiUse != GeminiUse.NONE && geminiConfig.isConfigured() && shouldCallGemini(result, email, role, geminiUse)) {
            log.debug("Refining classification for email {} with Gemini ({})", email.getId(), geminiUse);
            String geminiRaw = callGemini(
                    buildSystemPrompt(user),
                    buildUserMessage(email, body),
                    "Email to classify",
                    true);
            JsonNode geminiResult = geminiRaw != null ? parseJson(geminiRaw) : null;
            if (geminiResult != null) {
                result = geminiResult;
            }
        }

        persistClassificationResult(email, user, result);
    }

    private boolean shouldCallGemini(JsonNode localResult, Email email, User.UserRole role, GeminiUse geminiUse) {
        if (geminiUse == GeminiUse.ALWAYS) {
            return true;
        }
        String category = localResult != null ? getText(localResult, "category") : null;
        String summary = localResult != null ? getText(localResult, "summary") : null;
        return RoleClassificationProfile.worthGeminiRefinement(category, summary, email, role);
    }

    private void persistClassificationResult(Email email, User user, JsonNode result) {
        User.UserRole role = user.getUserRole() != null ? user.getUserRole() : User.UserRole.STUDENT;

        EmailCategory parsed = result != null ? parseCategory(result) : EmailCategory.UNCATEGORIZED;
        EmailCategory resolved = RoleClassificationProfile.resolveCategory(parsed, email, role);
        if (resolved == EmailCategory.UNCATEGORIZED) {
            resolved = RoleClassificationProfile.inferFromGmailAndSender(email, role);
            log.debug("Gmail fallback category {} applied to email {}", resolved, email.getId());
        }
        if (resolved == EmailCategory.UNCATEGORIZED) {
            resolved = EmailCategory.PERSONAL;
            log.warn("Classification exhausted for email {} — defaulting to PERSONAL", email.getId());
        }
        email.setCategory(resolved);

        if (result != null) {
            email.setPriority(parsePriority(result));
            email.setAiSummary(getText(result, "summary"));
            email.setAiActionItems(result.has("action_items") ? result.get("action_items").toString() : null);

            String deadlineStr = getText(result, "deadline");
            if (deadlineStr != null && !deadlineStr.equalsIgnoreCase("null")) {
                LocalDateTime deadline = parseDeadlineFlexible(deadlineStr);
                if (deadline != null) {
                    email.setDeadlineDetected(deadline);
                }
            }
        } else {
            email.setPriority(Boolean.TRUE.equals(email.getIsImportant()) ? Priority.HIGH : Priority.MEDIUM);
        }

        emailRepository.save(email);

        if (result != null && result.has("action_items") && result.get("action_items").isArray()) {
            saveActionItems(result.get("action_items"), email, user.getId());
        }

        if (email.getDeadlineDetected() != null) {
            calendarService.createDeadlineEvent(user, email);
        }
    }

    private static String emailBodyForClassification(Email email) {
        String body = email.getBodyFull() != null ? email.getBodyFull() : email.getBodySnippet();
        if (body == null) body = "";
        if (body.length() > 4000) body = body.substring(0, 4000);
        return body;
    }

    private String getLocalFallbackResponse(Email email, User.UserRole role) {
        String subject = (email.getSubject() != null ? email.getSubject() : "").toLowerCase();
        String body = (email.getBodyFull() != null ? email.getBodyFull()
                : (email.getBodySnippet() != null ? email.getBodySnippet() : "")).toLowerCase();
        String sender = (email.getSenderEmail() != null ? email.getSenderEmail() : "").toLowerCase();
        String labels = (email.getGmailLabelIds() != null ? email.getGmailLabelIds() : "").toUpperCase();
        String rawText = (email.getSubject() != null ? email.getSubject() : "") + "\n"
                + (email.getBodyFull() != null ? email.getBodyFull()
                : (email.getBodySnippet() != null ? email.getBodySnippet() : ""));

        String category = "UNCATEGORIZED";
        String priority = Boolean.TRUE.equals(email.getIsImportant()) ? "HIGH" : "MEDIUM";
        String summary = "Email from "
                + (email.getSenderName() != null ? email.getSenderName() : email.getSenderEmail())
                + (email.getSubject() != null ? (": " + email.getSubject()) : "") + ".";
        String actionType = "REVIEW";
        String actionDesc = "Review this email";
        String deadline = extractExplicitDeadlineIso(rawText);

        boolean marketing = isMarketingMail(sender, subject, body, labels);

        // 1) Gmail label source (matches the account's own tabs)
        if (labels.contains("SPAM")) {
            category = "SPAM";
            priority = "LOW";
            actionDesc = "Ignore or delete spam";
        } else if (labels.contains("CATEGORY_PROMOTIONS")) {
            category = "PROMOTIONAL";
            priority = "LOW";
            actionDesc = "Optional promo — review if relevant";
        } else if (labels.contains("CATEGORY_PURCHASES")
                || (labels.contains("CATEGORY_UPDATES")
                && (subject.contains("order") || subject.contains("receipt") || subject.contains("shipped")))) {
            category = "FINANCE";
            priority = "MEDIUM";
            actionDesc = "Review purchase or order update";
        } else if (labels.contains("CATEGORY_SOCIAL")) {
            category = "PERSONAL";
            priority = "LOW";
            actionDesc = "Social update — skim when free";
        } else if (labels.contains("CATEGORY_FORUMS")) {
            category = "ANNOUNCEMENT";
            priority = "LOW";
            actionDesc = "Forum / list update";
        } else if (marketing) {
            category = "PROMOTIONAL";
            priority = "LOW";
            actionDesc = "Promo — archive if not needed";
        }
        // 2) Sender domain / source
        else if (sender.contains("linkedin.") || sender.contains("github.") || sender.contains("twitter.")
                || sender.contains("facebook.") || sender.contains("instagram.")) {
            category = "PERSONAL";
            priority = "LOW";
        } else if (sender.contains("noreply") && (sender.contains("amazon.") || sender.contains("flipkart.")
                || sender.contains("ebay.") || sender.contains("shopify.") || sender.contains("paypal."))) {
            category = "FINANCE";
            priority = "MEDIUM";
            actionDesc = "Review purchase notification";
        } else if (sender.endsWith(".edu") || sender.contains("university") || sender.contains("college")
                || sender.contains("placement@") || sender.contains("careers@")) {
            if (subject.contains("placement") || subject.contains("interview") || body.contains("placement")) {
                category = "PLACEMENT";
                priority = "HIGH";
                actionType = "REPLY";
                actionDesc = "Reply to placement / careers";
            } else if (looksLikeAssignment(subject, body, sender)) {
                category = "ASSIGNMENT";
                priority = "HIGH";
                actionType = "SUBMIT";
                actionDesc = "Complete and submit your assignment";
            } else if (subject.contains("attendance") || body.contains("attendance")) {
                category = "ATTENDANCE";
                priority = "MEDIUM";
                actionDesc = "Check attendance notice";
            } else {
                category = "ANNOUNCEMENT";
                priority = "MEDIUM";
                actionDesc = "Review academic notice";
            }
        }
        // 3) Content keywords
        else if (looksLikeAssignment(subject, body, sender)) {
            category = "ASSIGNMENT";
            priority = "HIGH";
            actionType = "SUBMIT";
            actionDesc = "Complete and submit your assignment";
        } else if (subject.contains("hackathon") || body.contains("hackathon")) {
            category = "HACKATHON";
            priority = "HIGH";
            actionType = "REGISTER";
            actionDesc = "Register for the event";
        } else if (subject.contains("interview") || subject.contains("placement") || body.contains("placement") || body.contains("job offer")) {
            category = "PLACEMENT";
            priority = "HIGH";
            actionType = "REPLY";
            actionDesc = "Reply to the recruiter or placement office";
        } else if (subject.contains("meeting") || body.contains("meeting") || subject.contains("zoom")
                || body.contains("google meet") || subject.contains("invite:")
                || (labels.contains("IMPORTANT")
                    && (body.contains("calendar") || subject.contains("rsvp")))) {
            category = "MEETING";
            priority = "MEDIUM";
            actionType = "ATTEND";
            actionDesc = "Attend the scheduled meeting";
        } else if (subject.contains("internship") || body.contains("internship")) {
            category = "INTERNSHIP";
            priority = "MEDIUM";
            actionDesc = "Review the internship opportunity";
        } else if (subject.contains("research") || body.contains("research paper") || body.contains("journal")) {
            category = "RESEARCH";
            priority = "MEDIUM";
            actionDesc = "Review research note";
        } else if (subject.contains("alert") || subject.contains("announcement") || body.contains("important announcement")) {
            category = "ANNOUNCEMENT";
            priority = "MEDIUM";
            actionDesc = "Review the announcement";
        } else if (subject.contains("bill") || subject.contains("invoice") || body.contains("payment due")
                || subject.contains("fee") || subject.contains("receipt") || subject.contains("order #")) {
            category = "FINANCE";
            priority = "HIGH";
            actionDesc = "Verify payment details";
        } else if (subject.contains("unsubscribe") || body.contains("view in browser") || body.contains("% off")) {
            category = "PROMOTIONAL";
            priority = "LOW";
            actionDesc = "Promo — archive if not needed";
        } else if (sender.contains("google.com") || sender.contains("accounts.google")
                || subject.contains("security") || subject.contains("verification")
                || subject.contains("sign-in") || subject.contains("sign in")) {
            category = "ANNOUNCEMENT";
            priority = Boolean.TRUE.equals(email.getIsImportant()) ? "HIGH" : "MEDIUM";
            actionDesc = "Review account or security notice";
        } else if (labels.contains("CATEGORY_PERSONAL")) {
            category = "PERSONAL";
            priority = "MEDIUM";
            actionDesc = "Personal mail — review when ready";
        } else if (labels.contains("CATEGORY_UPDATES")) {
            category = "ANNOUNCEMENT";
            priority = "MEDIUM";
            actionDesc = "Account or service update";
        }

        category = applyRoleCategoryHints(category, role, subject, body, sender, marketing);
        summary = enrichSummary(category, email, summary, actionDesc);

        if (Boolean.TRUE.equals(email.getIsImportant()) && "LOW".equals(priority)) {
            priority = "MEDIUM";
        }

        String deadlineJson = deadline == null ? "null" : "\"" + deadline + "\"";
        boolean actionable = !"PROMOTIONAL".equals(category) && !"SPAM".equals(category)
                && !"UNCATEGORIZED".equals(category);

        String actionItemsJson = actionable
                ? """
          "action_items": [
            {
              "action_type": "%s",
              "description": "%s",
              "deadline": %s
            }
          ],
        """.formatted(actionType, jsonEscape(actionDesc), deadlineJson)
                : "\"action_items\": [],\n";

        return """
        {
          "category": "%s",
          "priority": "%s",
          "summary": "%s",
          %s
          "deadline": %s
        }
        """.formatted(
                category,
                priority,
                jsonEscape(summary),
                actionItemsJson,
                deadlineJson);
    }

    /** Escape user-derived strings before embedding in JSON text. */
    private static String jsonEscape(String value) {
        if (value == null) return "";
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", " ")
                .replace("\n", " ");
    }

    /**
     * Extract an explicit date/time from email text. Returns ISO-8601 local datetime or null.
     * Never invents a date that is not present in the text.
     */
    private String extractExplicitDeadlineIso(String text) {
        if (text == null || text.isBlank()) return null;

        // ISO-like: 2026-09-15 or 2026-09-15T15:00
        java.util.regex.Matcher iso = java.util.regex.Pattern
                .compile("\\b(20\\d{2})-(\\d{2})-(\\d{2})(?:[T ](\\d{1,2}):(\\d{2}))?\\b")
                .matcher(text);
        if (iso.find()) {
            return formatCapturedDateTime(iso.group(1), iso.group(2), iso.group(3), iso.group(4), iso.group(5));
        }

        // e.g. September 15, 2026 3:00 PM  /  Sep 15 2026
        java.util.regex.Matcher named = java.util.regex.Pattern
                .compile("(?i)\\b(january|february|march|april|may|june|july|august|september|october|november|december|jan|feb|mar|apr|jun|jul|aug|sep|sept|oct|nov|dec)\\.?\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:,)?\\s+(20\\d{2})(?:\\s+(?:at\\s+)?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm))?\\b")
                .matcher(text);
        if (named.find()) {
            Integer month = monthNumber(named.group(1));
            if (month != null) {
                String hour = named.group(4);
                String minute = named.group(5);
                String ampm = named.group(6);
                if (hour != null && ampm != null) {
                    int h = Integer.parseInt(hour);
                    if (ampm.equalsIgnoreCase("pm") && h < 12) h += 12;
                    if (ampm.equalsIgnoreCase("am") && h == 12) h = 0;
                    hour = String.format("%02d", h);
                }
                return formatCapturedDateTime(
                        named.group(3),
                        String.format("%02d", month),
                        String.format("%02d", Integer.parseInt(named.group(2))),
                        hour,
                        minute != null ? minute : (hour != null ? "00" : null));
            }
        }

        // e.g. 15/09/2026 or 15-09-2026 (day-month-year)
        java.util.regex.Matcher dmy = java.util.regex.Pattern
                .compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](20\\d{2})(?:\\s+(?:at\\s+)?(\\d{1,2}):(\\d{2}))?\\b")
                .matcher(text);
        if (dmy.find()) {
            int a = Integer.parseInt(dmy.group(1));
            int b = Integer.parseInt(dmy.group(2));
            // Prefer day-month when first number > 12
            String day;
            String month;
            if (a > 12) {
                day = String.format("%02d", a);
                month = String.format("%02d", b);
            } else if (b > 12) {
                month = String.format("%02d", a);
                day = String.format("%02d", b);
            } else {
                // Ambiguous — treat as day/month (common outside US)
                day = String.format("%02d", a);
                month = String.format("%02d", b);
            }
            return formatCapturedDateTime(dmy.group(3), month, day, dmy.group(4), dmy.group(5));
        }

        return null;
    }

    private String formatCapturedDateTime(String year, String month, String day, String hour, String minute) {
        try {
            int y = Integer.parseInt(year);
            int m = Integer.parseInt(month);
            int d = Integer.parseInt(day);
            int h = hour != null ? Integer.parseInt(hour) : 0;
            int min = minute != null ? Integer.parseInt(minute) : 0;
            LocalDateTime dt = LocalDateTime.of(y, m, d, h, min);
            return dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer monthNumber(String name) {
        if (name == null) return null;
        return switch (name.toLowerCase().replace(".", "")) {
            case "january", "jan" -> 1;
            case "february", "feb" -> 2;
            case "march", "mar" -> 3;
            case "april", "apr" -> 4;
            case "may" -> 5;
            case "june", "jun" -> 6;
            case "july", "jul" -> 7;
            case "august", "aug" -> 8;
            case "september", "sep", "sept" -> 9;
            case "october", "oct" -> 10;
            case "november", "nov" -> 11;
            case "december", "dec" -> 12;
            default -> null;
        };
    }

    private String enrichSummary(String category, Email email, String summary, String actionDesc) {
        String sender = email.getSenderName() != null ? email.getSenderName() : email.getSenderEmail();
        String subject = email.getSubject() != null ? email.getSubject() : "(no subject)";
        if (summary != null && !summary.startsWith("Email from ")) {
            return summary;
        }
        return switch (category) {
            case "ASSIGNMENT" -> sender + " sent coursework: \"" + subject + "\". " + actionDesc + ".";
            case "PLACEMENT" -> "Careers / recruiting from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            case "HACKATHON" -> "Event mail from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            case "MEETING" -> "Meeting invite from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            case "RESEARCH" -> "Research note from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            case "FINANCE" -> "Billing or payment from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            case "PROMOTIONAL" -> "Promotion from " + sender + ": \"" + subject + "\".";
            case "ANNOUNCEMENT" -> "Notice from " + sender + ": \"" + subject + "\". " + actionDesc + ".";
            default -> summary;
        };
    }

    /** Backward-compatible wrapper for unstructured text prompts. */
    public String callGemini(String systemPrompt, String userMessage) {
        return callGemini(systemPrompt, userMessage, "Input", false);
    }

    private void recategorizeMarketingMislabels(Long userId) {
        var miscategorized = emailRepository.findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
                userId,
                EmailCategory.ASSIGNMENT,
                org.springframework.data.domain.PageRequest.of(0, 200)
        ).getContent();
        for (Email email : miscategorized) {
            String subject = (email.getSubject() != null ? email.getSubject() : "").toLowerCase();
            String body = (email.getBodyFull() != null ? email.getBodyFull()
                    : (email.getBodySnippet() != null ? email.getBodySnippet() : "")).toLowerCase();
            String sender = (email.getSenderEmail() != null ? email.getSenderEmail() : "").toLowerCase();
            String labels = (email.getGmailLabelIds() != null ? email.getGmailLabelIds() : "").toUpperCase();
            if (isMarketingMail(sender, subject, body, labels) && !looksLikeAssignment(subject, body, sender)) {
                email.setCategory(EmailCategory.PROMOTIONAL);
                emailRepository.save(email);
            }
        }
    }

    private boolean isMarketingMail(String sender, String subject, String body, String labels) {
        if (labels.contains("CATEGORY_PROMOTIONS")) return true;
        if (body.contains("unsubscribe") || body.contains("view in browser") || body.contains("% off")
                || body.contains("limited time offer") || body.contains("no longer wish to receive")) {
            return true;
        }
        return sender.contains("paperpal") || sender.contains("grammarly") || sender.contains("mailchimp")
                || sender.contains("sendgrid") || sender.contains("campaign") || sender.contains("newsletter")
                || sender.contains("marketing") || sender.contains("promo");
    }

    private boolean looksLikeAssignment(String subject, String body, String sender) {
        if (subject.contains("assignment") || subject.contains("homework") || subject.contains("coursework")
                || subject.contains("lab report") || subject.contains("project submission")) {
            return true;
        }
        boolean academicSender = sender.contains(".edu") || sender.contains("university") || sender.contains("college")
                || sender.contains("canvas.") || sender.contains("moodle") || sender.contains("blackboard")
                || sender.contains("classroom.google");
        if (academicSender && (body.contains("assignment") || body.contains("homework")
                || body.contains("due date") || body.contains("submit your work")
                || body.contains("submit the assignment"))) {
            return true;
        }
        return false;
    }

    /** Nudge ambiguous mail into divisions that fit the signed-in account type. */
    private String applyRoleCategoryHints(String category, User.UserRole role,
                                          String subject, String body, String sender, boolean marketing) {
        if (marketing && !"SPAM".equals(category)) {
            return "PROMOTIONAL";
        }
        if (!"UNCATEGORIZED".equals(category)) {
            return category;
        }
        return switch (role) {
            case STUDENT -> {
                if (subject.contains("placement") || body.contains("placement")) yield "PLACEMENT";
                if (subject.contains("hackathon") || body.contains("hackathon")) yield "HACKATHON";
                if (looksLikeAssignment(subject, body, sender)) yield "ASSIGNMENT";
                if (sender.contains(".edu") || sender.contains("university")) yield "ANNOUNCEMENT";
                yield category;
            }
            case PROFESSOR -> {
                if (subject.contains("research") || body.contains("research")) yield "RESEARCH";
                if (subject.contains("meeting") || body.contains("meeting")) yield "MEETING";
                yield category;
            }
            case HR_PROFESSIONAL -> {
                if (subject.contains("interview") || subject.contains("candidate") || body.contains("resume")) yield "PLACEMENT";
                if (subject.contains("internship")) yield "INTERNSHIP";
                yield category;
            }
            case FREELANCER -> {
                if (subject.contains("invoice") || subject.contains("payment") || body.contains("invoice")) yield "FINANCE";
                yield category;
            }
            case IT_EMPLOYEE -> {
                if (subject.contains("incident") || subject.contains("alert") || body.contains("on-call")) yield "ANNOUNCEMENT";
                if (subject.contains("jira") || body.contains("ticket")) yield "MEETING";
                yield category;
            }
            case MANAGER -> {
                if (subject.contains("approval") || subject.contains("budget") || body.contains("headcount")) yield "FINANCE";
                if (subject.contains("1:1") || subject.contains("sync")) yield "MEETING";
                yield category;
            }
            default -> category;
        };
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

    private LocalDateTime parseDeadlineFlexible(String deadlineStr) {
        if (deadlineStr == null || deadlineStr.isBlank() || deadlineStr.equalsIgnoreCase("null")) {
            return null;
        }
        String cleaned = deadlineStr.trim().replace("Z", "");
        if (cleaned.length() > 19) {
            cleaned = cleaned.substring(0, 19);
        }
        try {
            return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            return LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception ignored) {}
        try {
            return java.time.LocalDate.parse(cleaned.substring(0, Math.min(10, cleaned.length())))
                    .atStartOfDay();
        } catch (Exception ignored) {}
        return null;
    }

    private void saveActionItems(JsonNode items, Email email, Long userId) {
        if (email.getId() != null) {
            List<EmailAction> existing = actionRepository.findByEmailId(email.getId());
            if (!existing.isEmpty()) {
                actionRepository.deleteAll(existing);
            }
        }
        for (JsonNode item : items) {
            try {
                EmailAction action = EmailAction.builder()
                        .email(email)
                        .userId(userId)
                        .actionType(parseActionType(getText(item, "action_type")))
                        .actionDescription(getText(item, "description") != null ? getText(item, "description") : "Action required")
                        .build();

                String deadline = getText(item, "deadline");
                if (deadline != null && !deadline.equalsIgnoreCase("null")) {
                    LocalDateTime parsed = parseDeadlineFlexible(deadline);
                    if (parsed != null) {
                        action.setDeadline(parsed);
                    }
                }
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
                String response = callGemini(
                        RoleClassificationProfile.buildProfessionDetectionPrompt(),
                        context.toString(),
                        "Recent emails",
                        true);
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
