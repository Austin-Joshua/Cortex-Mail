package com.nexora.service;

import com.nexora.dto.response.SenderSummaryResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import com.nexora.model.Email.Reaction;
import com.nexora.repository.EmailRepository;
import com.nexora.dto.response.EmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import org.springframework.scheduling.annotation.Async;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final EmailRepository emailRepository;
    private final GmailSyncService gmailSyncService;
    private final EmailClassificationService classificationService;

    public Page<EmailResponse> getEmails(Long userId, String category, String priority,
                                          String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());

        Page<Email> emailPage;

        if (search != null && !search.isBlank()) {
            emailPage = emailRepository.searchByUserId(userId, search, pageable);
        } else if (category != null && priority != null) {
            emailPage = emailRepository.findByUserIdAndCategoryAndPriorityOrderByReceivedAtDesc(
                    userId, EmailCategory.valueOf(category), Priority.valueOf(priority), pageable);
        } else if (category != null) {
            emailPage = emailRepository.findByUserIdAndCategoryOrderByReceivedAtDesc(
                    userId, EmailCategory.valueOf(category), pageable);
        } else if (priority != null) {
            emailPage = emailRepository.findByUserIdAndPriorityOrderByReceivedAtDesc(
                    userId, Priority.valueOf(priority), pageable);
        } else {
            emailPage = emailRepository.findByUserIdOrderByReceivedAtDesc(userId, pageable);
        }

        return emailPage.map(e -> toResponse(e, false));
    }

    public EmailResponse getEmailDetail(Long userId, Long emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        return toResponse(email, true);
    }

    public void markRead(Long userId, Long emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        if (!Boolean.TRUE.equals(email.getIsRead())) {
            email.setIsRead(true);
            emailRepository.save(email);
            // Propagate mark-as-read back to Gmail asynchronously
            if (email.getGmailMessageId() != null) {
                java.util.concurrent.CompletableFuture.runAsync(() ->
                        gmailSyncService.markReadInGmail(userId, email.getGmailMessageId()));
            }
        }
    }

    public void updateReaction(Long userId, Long emailId, String reaction) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        try {
            email.setReaction(Reaction.valueOf(reaction.toUpperCase()));
            emailRepository.save(email);
        } catch (IllegalArgumentException ex) {
            throw new NexoraException("Invalid reaction: " + reaction, 400);
        }
    }

    @Async
    public void triggerSync(Long userId) {
        gmailSyncService.syncInbox(userId);
    }

    public void syncInbox(Long userId) {
        gmailSyncService.syncInbox(userId);
    }

    public Map<String, Long> getCategoryCounts(Long userId) {
        List<Object[]> results = emailRepository.countByUserIdGroupByCategory(userId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : results) {
            counts.put(row[0].toString(), (Long) row[1]);
        }
        return counts;
    }

    /**
     * Returns a ranked list of senders grouped by email count, descending.
     */
    public List<SenderSummaryResponse> getSenderSummary(Long userId) {
        List<Object[]> rows = emailRepository.countBySenderForUser(userId);
        return rows.stream().map(row -> SenderSummaryResponse.builder()
                .senderEmail((String) row[0])
                .senderName((String) row[1])
                .emailCount((Long) row[2])
                .latestReceivedAt((LocalDateTime) row[3])
                .latestSubject((String) row[4])
                .build()
        ).collect(Collectors.toList());
    }

    /**
     * Returns paginated emails from a specific sender for a user.
     */
    public Page<EmailResponse> getEmailsBySender(Long userId, String senderEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        Page<Email> emailPage = emailRepository
                .findByUserIdAndSenderEmailOrderByReceivedAtDesc(userId, senderEmail, pageable);
        return emailPage.map(e -> toResponse(e, false));
    }

    public EmailResponse toResponse(Email email, boolean includeFullBody) {
        List<EmailResponse.ActionItemDto> actions = new ArrayList<>();
        if (email.getActions() != null) {
            actions = email.getActions().stream().map(a -> EmailResponse.ActionItemDto.builder()
                    .id(a.getId())
                    .actionType(a.getActionType())
                    .actionDescription(a.getActionDescription())
                    .deadline(a.getDeadline())
                    .isCompleted(a.getIsCompleted())
                    .build()).collect(Collectors.toList());
        }

        return EmailResponse.builder()
                .id(email.getId())
                .gmailMessageId(email.getGmailMessageId())
                .gmailThreadId(email.getGmailThreadId())
                .senderName(email.getSenderName())
                .senderEmail(email.getSenderEmail())
                .subject(email.getSubject())
                .bodySnippet(email.getBodySnippet())
                .bodyFull(includeFullBody ? email.getBodyFull() : null)
                .receivedAt(email.getReceivedAt())
                .isRead(email.getIsRead())
                .hasAttachments(email.getHasAttachments())
                .category(email.getCategory())
                .priority(email.getPriority())
                .reaction(email.getReaction() != null ? email.getReaction() : Reaction.NONE)
                .aiSummary(email.getAiSummary())
                .aiActionItems(email.getAiActionItems())
                .deadlineDetected(email.getDeadlineDetected())
                .isDeadlineAddedToCalendar(email.getIsDeadlineAddedToCalendar())
                .actions(actions)
                .createdAt(email.getCreatedAt())
                .build();
    }

    public List<Map<String, Object>> getEmailVolume(Long userId, int days) {
        LocalDateTime start = LocalDateTime.now().minusDays(days - 1L).withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<LocalDateTime> dates = emailRepository.findReceivedAtByUserIdAndReceivedAtAfter(userId, start);

        Map<String, Long> grouped = new LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = days - 1; i >= 0; i--) {
            grouped.put(java.time.LocalDate.now().minusDays(i).format(formatter), 0L);
        }

        for (LocalDateTime dt : dates) {
            String key = dt.toLocalDate().format(formatter);
            if (grouped.containsKey(key)) {
                grouped.put(key, grouped.get(key) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            result.add(Map.of("date", entry.getKey(), "count", entry.getValue()));
        }
        return result;
    }

    public List<EmailResponse> getEmailThread(Long userId, String threadId) {
        List<Email> emails = emailRepository.findByUserIdAndGmailThreadIdOrderByReceivedAtAsc(userId, threadId);
        return emails.stream().map(e -> toResponse(e, false)).collect(Collectors.toList());
    }

    public String draftReply(Long userId, Long emailId, String style) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));

        String body = email.getBodyFull() != null ? email.getBodyFull() : email.getBodySnippet();
        if (body == null) body = "";
        if (body.length() > 2000) body = body.substring(0, 2000);

        String systemPrompt = "Draft a " + style + " email reply. Return ONLY the reply body text. "
                + "No subject line. No 'Dear...' unless formal. No sign-off unless formal. No HTML tags.";

        String userMessage = "Original email from " + (email.getSenderName() != null ? email.getSenderName() : email.getSenderEmail())
                + ":\nSubject: " + (email.getSubject() != null ? email.getSubject() : "")
                + "\n\n" + body;

        String draft = classificationService.generateBrainAnswer(systemPrompt, userMessage);
        if (draft != null && !draft.isBlank()) {
            return draft.trim();
        }

        return "Hi " + (email.getSenderName() != null ? email.getSenderName() : "there") + ",\n\n"
                + "Thanks for your email regarding \"" + (email.getSubject() != null ? email.getSubject() : "") + "\".\n"
                + "I'll review this and get back to you shortly.\n\n"
                + "Regards,\n[Your Name]";
    }

    public void sendReply(Long userId, Long emailId, String replyBody) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        log.info("Sending reply for user {} on email {}: {}", userId, emailId, replyBody);
        // Persist sent status / trigger Gmail API sending
        email.setReaction(Reaction.DONE);
        emailRepository.save(email);
    }

    // ---------------------------------------------------------------- priority

    /**
     * Unread mail ordered by how much it needs the user: HIGH first, then
     * MEDIUM, then LOW, newest first inside each band. Backs /api/priority.
     */
    public List<Email> getPriorityEmails(Long userId, int limit) {
        List<Email> out = new ArrayList<>();
        for (Priority p : List.of(Priority.HIGH, Priority.MEDIUM, Priority.LOW)) {
            if (out.size() >= limit) break;
            List<Email> band = emailRepository
                    .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                            userId, p, PageRequest.of(0, limit));
            for (Email e : band) {
                if (out.size() >= limit) break;
                out.add(e);
            }
        }
        return out;
    }

    /** Pins an email to the top of Priority by marking it IMPORTANT. */
    public Email flagAsImportant(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        email.setReaction(Reaction.IMPORTANT);
        email.setPriority(Priority.HIGH);
        return emailRepository.save(email);
    }

    /** Clears the IMPORTANT pin, leaving the classifier's own priority. */
    public Email unflagAsImportant(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getReaction() == Reaction.IMPORTANT) {
            email.setReaction(Reaction.NONE);
        }
        return emailRepository.save(email);
    }

    /**
     * Unread mail the classifier did not rank HIGH but which carries a
     * deadline inside the next week — the cases most likely to be missed.
     */
    public List<Email> getSuggestedPriorityEmails(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(7);
        List<Email> candidates = new ArrayList<>();
        candidates.addAll(emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Priority.MEDIUM, PageRequest.of(0, 50)));
        candidates.addAll(emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Priority.LOW, PageRequest.of(0, 50)));

        return candidates.stream()
                .filter(e -> e.getDeadlineDetected() != null)
                .filter(e -> !e.getDeadlineDetected().isBefore(now)
                        && e.getDeadlineDetected().isBefore(horizon))
                .sorted(Comparator.comparing(Email::getDeadlineDetected))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Email ownedEmail(Long userId, Long emailId) {
        return emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email " + emailId + " not found"));
    }
}
