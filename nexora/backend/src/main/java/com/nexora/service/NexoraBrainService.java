package com.nexora.service;

import com.nexora.dto.response.BrainConversationResponse;
import com.nexora.dto.response.BrainQueryResponse;
import com.nexora.dto.response.EmailResponse;
import com.nexora.model.BrainConversation;
import com.nexora.model.Email;
import com.nexora.repository.BrainConversationRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import com.nexora.exception.NexoraException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class NexoraBrainService {

    private final EmailRepository emailRepository;
    private final BrainConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final EmailClassificationService classificationService;
    private final EmailService emailService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    public NexoraBrainService(EmailRepository emailRepository,
                              BrainConversationRepository conversationRepository,
                              UserRepository userRepository,
                              EmailClassificationService classificationService,
                              EmailService emailService) {
        this.emailRepository = emailRepository;
        this.conversationRepository = conversationRepository;
        this.userRepository = userRepository;
        this.classificationService = classificationService;
        this.emailService = emailService;
    }

    public BrainQueryResponse query(Long userId, String userQuery) {
        if (!userRepository.existsById(userId)) {
            throw new NexoraException("User not found", 404);
        }

        // Retrieve: keyword search over the mailbox, then recent mail as fallback (RAG-lite).
        List<Email> retrieved = retrieveRelevantMail(userId, userQuery);

        String emailContext = buildEmailContext(retrieved);

        // Step 3: Call Gemini (keyword fallback if key unset)
        String systemPrompt = """
You are Cortex Mail Brain, a personal communication assistant. You have access to the user's recent emails (summarized below). Answer the user's question based ONLY on the information in these emails. Be specific — mention sender names, dates, and subject lines when relevant. If the answer is not found in the emails, say so clearly. Never invent emails, deadlines, or events that are not present.

User's email history:
%s
""".formatted(emailContext);

        String answer = classificationService.generateBrainAnswer(systemPrompt, userQuery);
        if (answer == null) {
            answer = generateLocalBrainAnswer(retrieved, userQuery);
        }

        // Step 4: Find referenced emails (simple keyword match)
        List<Email> referenced = findReferencedEmails(retrieved, userQuery, answer);

        // Step 5: Save conversation
        List<Long> refIds = referenced.stream()
                .map(email -> email.getId())
                .filter(id -> id != null)
                .collect(Collectors.toList());
        BrainConversation conversation = BrainConversation.builder()
                .userId(userId)
                .userQuery(userQuery)
                .aiResponse(answer)
                .referencedEmailIds(refIds.toString())
                .build();
        conversation = conversationRepository.save(conversation);

        List<EmailResponse> refResponses = referenced.stream()
                .map(e -> emailService.toResponse(e, false))
                .collect(Collectors.toList());

        return BrainQueryResponse.builder()
                .answer(answer)
                .referencedEmails(refResponses)
                .conversationId(conversation.getId())
                .build();
    }

    public List<BrainConversationResponse> getHistory(Long userId) {
        return conversationRepository.findByUserIdOrderByCreatedAtDesc(userId,
                org.springframework.data.domain.PageRequest.of(0, 20)).stream()
                .map(c -> {
                    BrainConversationResponse r = new BrainConversationResponse();
                    r.setId(c.getId());
                    r.setUserQuery(c.getUserQuery());
                    r.setAiResponse(c.getAiResponse());
                    r.setReferencedEmailIds(c.getReferencedEmailIds());
                    r.setCreatedAt(c.getCreatedAt());
                    return r;
                })
                .collect(Collectors.toList());
    }

    /**
     * Keyword retrieval over stored mail (not embeddings). Caps context so Gemini
     * stays fast while answers still come from the user's actual mailbox.
     */
    private List<Email> retrieveRelevantMail(Long userId, String userQuery) {
        LinkedHashMap<Long, Email> byId = new LinkedHashMap<>();
        String hay = userQuery != null ? userQuery.trim() : "";
        if (hay.length() >= 3) {
            String token = hay.length() > 80 ? hay.substring(0, 80) : hay;
            for (Email e : emailRepository.searchByUserId(
                    userId, token, org.springframework.data.domain.PageRequest.of(0, 30)).getContent()) {
                if (e.getId() != null) byId.put(e.getId(), e);
            }
        }
        for (Email e : emailRepository.findTop80ByUserIdOrderByReceivedAtDesc(userId)) {
            if (e.getId() != null) byId.putIfAbsent(e.getId(), e);
        }
        List<Email> out = new ArrayList<>(byId.values());
        return out.size() > 40 ? out.subList(0, 40) : out;
    }

    private String buildEmailContext(List<Email> emails) {
        StringBuilder sb = new StringBuilder();
        for (Email e : emails) {
            sb.append("[").append(e.getReceivedAt() != null ? e.getReceivedAt().format(DATE_FMT) : "Unknown date").append("] ");
            sb.append("FROM: ").append(e.getSenderName() != null ? e.getSenderName() : "").append(" <").append(e.getSenderEmail()).append("> | ");
            sb.append("SUBJECT: ").append(e.getSubject() != null ? e.getSubject() : "(no subject)").append(" | ");
            sb.append("CATEGORY: ").append(e.getCategory()).append(" | ");
            sb.append("PRIORITY: ").append(e.getPriority()).append(" | ");
            if (e.getAiSummary() != null) {
                sb.append("SUMMARY: ").append(e.getAiSummary());
            } else if (e.getBodySnippet() != null) {
                sb.append("SNIPPET: ").append(e.getBodySnippet());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private List<Email> findReferencedEmails(List<Email> emails, String query, String answer) {
        // Return emails whose subject/sender appears in the AI answer
        return emails.stream()
                .filter(e -> {
                    String sub = e.getSubject() != null ? e.getSubject().toLowerCase() : "";
                    String sender = e.getSenderName() != null ? e.getSenderName().toLowerCase() : "";
                    String ans = answer.toLowerCase();
                    return (!sub.isEmpty() && ans.contains(sub.substring(0, Math.min(sub.length(), 10)))) ||
                           (!sender.isEmpty() && ans.contains(sender.split(" ")[0].toLowerCase()));
                })
                .limit(5)
                .collect(Collectors.toList());
    }

    private String generateLocalBrainAnswer(List<Email> emails, String userQuery) {
        if (emails == null || emails.isEmpty()) {
            return "Your inbox has no synced emails yet. Click Sync inbox after connecting Gmail, then ask again.";
        }

        String q = userQuery.toLowerCase();
        List<Email> matches = emails.stream()
                .filter(e -> {
                    String hay = ((e.getSubject() != null ? e.getSubject() : "") + " " +
                            (e.getSenderName() != null ? e.getSenderName() : "") + " " +
                            (e.getAiSummary() != null ? e.getAiSummary() : "") + " " +
                            (e.getBodySnippet() != null ? e.getBodySnippet() : "") + " " +
                            (e.getCategory() != null ? e.getCategory().name() : "")).toLowerCase();
                    if (q.contains("deadline") || q.contains("due")) {
                        return e.getDeadlineDetected() != null;
                    }
                    if (q.contains("assignment")) return hay.contains("assignment") || (e.getCategory() != null && e.getCategory().name().equals("ASSIGNMENT"));
                    if (q.contains("meeting")) return hay.contains("meeting") || (e.getCategory() != null && e.getCategory().name().equals("MEETING"));
                    if (q.contains("interview") || q.contains("placement") || q.contains("job")) {
                        return hay.contains("interview") || hay.contains("placement") || (e.getCategory() != null && e.getCategory().name().equals("PLACEMENT"));
                    }
                    if (q.contains("hackathon")) return hay.contains("hackathon") || (e.getCategory() != null && e.getCategory().name().equals("HACKATHON"));
                    // Generic: match any query token against subject/sender
                    return Arrays.stream(q.split("\\s+"))
                            .filter(t -> t.length() > 3)
                            .anyMatch(hay::contains);
                })
                .limit(5)
                .collect(Collectors.toList());

        if (matches.isEmpty()) {
            return "I checked your " + emails.size() + " most recent synced emails and did not find a clear match for that question. Try asking about a sender, subject, or deadline that appears in your inbox.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Based only on your synced Gmail messages:\n");
        for (Email e : matches) {
            sb.append("• ").append(e.getSubject() != null ? e.getSubject() : "(no subject)");
            sb.append(" — from ").append(e.getSenderName() != null ? e.getSenderName() : e.getSenderEmail());
            if (e.getDeadlineDetected() != null) {
                sb.append(" — date/time in mail: ").append(e.getDeadlineDetected().format(DATE_FMT));
            } else if (e.getReceivedAt() != null) {
                sb.append(" — received ").append(e.getReceivedAt().format(DATE_FMT));
            }
            if (e.getAiSummary() != null && !e.getAiSummary().isBlank()) {
                sb.append(" — ").append(e.getAiSummary());
            }
            sb.append("\n");
        }
        return sb.toString().trim();
    }
}
