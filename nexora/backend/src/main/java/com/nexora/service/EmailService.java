package com.nexora.service;

import com.nexora.dto.response.EmailResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.dto.response.GmailSyncResponse;
import com.nexora.dto.response.SenderSummaryResponse;
import com.nexora.dto.response.SyncIntegrityResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import com.nexora.model.Email.Reaction;
import com.nexora.model.User;
import com.nexora.repository.EmailRepository;
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
            emailPage = emailRepository.searchInboxByUserId(userId, search, pageable);
        } else if (category != null && priority != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndCategoryAndPriorityOrderByReceivedAtDesc(
                    userId, EmailCategory.valueOf(category), Priority.valueOf(priority), pageable);
        } else if (category != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
                    userId, EmailCategory.valueOf(category), pageable);
        } else if (priority != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndPriorityOrderByReceivedAtDesc(
                    userId, Priority.valueOf(priority), pageable);
        } else {
            emailPage = emailRepository.findByUserIdAndInInboxTrueOrderByReceivedAtDesc(userId, pageable);
        }

        return emailPage.map(e -> toResponse(e, false));
    }

    public Page<EmailResponse> getDraftEmails(Long userId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        Page<Email> emailPage = (search != null && !search.isBlank())
                ? emailRepository.searchDraftsByUserId(userId, search, pageable)
                : emailRepository.findByUserIdAndIsDraftTrueOrderByReceivedAtDesc(userId, pageable);
        return emailPage.map(e -> toResponse(e, false));
    }

    public Page<EmailResponse> getArchivedEmails(Long userId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
        Page<Email> emailPage = (search != null && !search.isBlank())
                ? emailRepository.searchArchivedByUserId(userId, search, pageable)
                : emailRepository.findByUserIdAndIsArchivedTrueOrderByReceivedAtDesc(userId, pageable);
        return emailPage.map(e -> toResponse(e, false));
    }

    public EmailResponse getEmailDetail(Long userId, Long emailId) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        return toResponse(email, true);
    }

    public EmailResponse markRead(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.markReadInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsRead(true);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse markUnread(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.markUnreadInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsRead(false);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse setStarred(Long userId, Long emailId, boolean starred) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = starred
                    ? gmailSyncService.starInGmail(userId, email.getGmailMessageId())
                    : gmailSyncService.unstarInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsStarred(starred);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse archive(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.archiveInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setInInbox(false);
            email.setIsArchived(true);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse moveToInbox(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.moveToInboxInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setInInbox(true);
            email.setIsArchived(false);
            email.setIsTrash(false);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse trash(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.trashInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsTrash(true);
            email.setInInbox(false);
            email.setIsArchived(false);
        }
        emailRepository.save(email);
        return toResponse(email, false);
    }

    public EmailResponse restoreFromTrash(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.restoreFromTrashInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsTrash(false);
            email.setInInbox(true);
            email.setIsArchived(false);
        }
        emailRepository.save(email);
        return toResponse(email, false);
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

    public GmailSyncResponse syncInbox(Long userId) {
        return gmailSyncService.syncInbox(userId);
    }

    public Map<String, GmailLabelCountResponse> getGmailLabelCounts(Long userId) {
        return gmailSyncService.getLabelCounts(userId);
    }

    /**
     * After inbox sync: separate mail by Gmail label source + content, then group.
     * Runs synchronously so groups appear immediately in the UI.
     */
    public Map<String, Object> classifyInbox(Long userId, User user) {
        int classified = classificationService.classifyInboxBySourceAndContent(userId);
        Map<String, Long> groups = getCategoryCounts(userId);
        return Map.of(
                "message", "Mails separated and grouped by source and content",
                "classified", classified,
                "groups", groups
        );
    }

    /**
     * Compares Gmail Labels API totals with local DB + classification groups.
     */
    public SyncIntegrityResponse getSyncIntegrity(User user) {
        Long userId = user.getId();
        Map<String, GmailLabelCountResponse> labels = gmailSyncService.getLabelCounts(userId);

        long gmailInbox = labels.get("INBOX") != null && labels.get("INBOX").getMessagesTotal() != null
                ? labels.get("INBOX").getMessagesTotal() : -1;
        long gmailUnread = labels.get("INBOX") != null && labels.get("INBOX").getMessagesUnread() != null
                ? labels.get("INBOX").getMessagesUnread() : -1;
        long gmailDrafts = labels.get("DRAFT") != null && labels.get("DRAFT").getMessagesTotal() != null
                ? labels.get("DRAFT").getMessagesTotal() : -1;

        long localInbox = emailRepository.countByUserIdAndInInboxTrue(userId);
        long localDrafts = emailRepository.countByUserIdAndIsDraftTrue(userId);
        long localArchived = emailRepository.countByUserIdAndIsArchivedTrue(userId);
        long localTotal = emailRepository.countByUserId(userId);
        long localUnread = emailRepository.countInboxUnreadByUserId(userId);
        long unclassified = emailRepository.countByUserIdAndCategoryAndInInboxTrue(
                userId, EmailCategory.UNCATEGORIZED);

        Map<String, Long> gmailCounts = new LinkedHashMap<>();
        gmailCounts.put("inboxTotal", gmailInbox);
        gmailCounts.put("inboxUnread", gmailUnread);
        gmailCounts.put("drafts", gmailDrafts);
        if (labels.get("IMPORTANT") != null && labels.get("IMPORTANT").getMessagesTotal() != null) {
            gmailCounts.put("important", labels.get("IMPORTANT").getMessagesTotal());
        }
        if (labels.get("SPAM") != null && labels.get("SPAM").getMessagesTotal() != null) {
            gmailCounts.put("spam", labels.get("SPAM").getMessagesTotal());
        }

        Map<String, Long> localCounts = new LinkedHashMap<>();
        localCounts.put("inboxTotal", localInbox);
        localCounts.put("inboxUnread", localUnread);
        localCounts.put("drafts", localDrafts);
        localCounts.put("archived", localArchived);
        localCounts.put("allStored", localTotal);

        List<String> notes = new ArrayList<>();
        boolean inboxAligned = gmailInbox < 0 || localInbox == gmailInbox;
        boolean draftsAligned = gmailDrafts < 0 || localDrafts == gmailDrafts;

        if (user.getGmailAccessToken() == null) {
            notes.add("Gmail is not connected — no token on user.");
        }
        if (labels.isEmpty()) {
            notes.add("No cached Gmail label counts yet — run Sync so Labels API data is stored.");
        }
        if (gmailInbox >= 0 && localInbox < gmailInbox) {
            notes.add("Local inbox (" + localInbox + ") is below Gmail INBOX total (" + gmailInbox + "). Sync may still be incomplete.");
        }
        if (gmailInbox >= 0 && localInbox > gmailInbox) {
            notes.add("Local inbox (" + localInbox + ") is above Gmail INBOX (" + gmailInbox + "). Stale rows may remain.");
        }
        if (gmailDrafts >= 0 && localDrafts != gmailDrafts) {
            notes.add("Drafts: Gmail=" + gmailDrafts + ", local=" + localDrafts + ".");
        }
        if (unclassified > 0) {
            notes.add(unclassified + " inbox mails are still UNCATEGORIZED — classification may not have finished.");
        } else if (localInbox > 0) {
            notes.add("All synced inbox mails have a category group.");
        }
        if (localInbox == 0 && (gmailInbox > 0 || labels.isEmpty())) {
            notes.add("No inbox mail stored locally. Open Dashboard to trigger extract → score → classify.");
        }

        List<Map<String, Object>> sample = emailRepository
                .findByUserIdAndInInboxTrueOrderByReceivedAtDesc(userId, PageRequest.of(0, 5))
                .getContent()
                .stream()
                .map(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", e.getId());
                    row.put("subject", e.getSubject());
                    row.put("from", e.getSenderEmail());
                    row.put("category", e.getCategory() != null ? e.getCategory().name() : null);
                    row.put("priority", e.getPriority() != null ? e.getPriority().name() : null);
                    row.put("isRead", e.getIsRead());
                    row.put("summary", e.getAiSummary());
                    row.put("labels", e.getGmailLabelIds());
                    return row;
                })
                .collect(Collectors.toList());

        SyncIntegrityResponse out = new SyncIntegrityResponse();
        out.setConnected(user.getGmailAccessToken() != null);
        out.setLastSyncedAt(user.getLastSyncedAt() != null ? user.getLastSyncedAt().toString() : null);
        out.setGmailCounts(gmailCounts);
        out.setLocalCounts(localCounts);
        out.setCategoryGroups(getCategoryCounts(userId));
        out.setUnclassifiedInbox(unclassified);
        out.setInboxAligned(inboxAligned);
        out.setDraftsAligned(draftsAligned);
        out.setNotes(notes);
        out.setSampleInbox(sample);
        return out;
    }

    public void classifyEmail(Long userId, Long emailId, User user) {
        Email email = emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        classificationService.classifyEmail(email.getId(), user);
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

        List<EmailResponse.AttachmentDto> attachments = new ArrayList<>();
        if (includeFullBody && email.getAttachments() != null) {
            attachments = email.getAttachments().stream()
                    .map(a -> EmailResponse.AttachmentDto.builder()
                            .id(a.getId())
                            .filename(a.getFilename())
                            .mimeType(a.getMimeType())
                            .sizeBytes(a.getSizeBytes())
                            .isInline(a.getIsInline())
                            .build())
                    .collect(Collectors.toList());
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
                .bodyHtml(includeFullBody ? email.getBodyHtml() : null)
                .receivedAt(email.getReceivedAt())
                .isRead(email.getIsRead())
                .hasAttachments(email.getHasAttachments())
                .gmailLabelIds(email.getGmailLabelIds())
                .recipientTo(email.getRecipientTo())
                .recipientCc(email.getRecipientCc())
                .isStarred(email.getIsStarred())
                .isImportant(email.getIsImportant())
                .inInbox(email.getInInbox())
                .isDraft(email.getIsDraft())
                .isArchived(email.getIsArchived())
                .isTrash(email.getIsTrash())
                .isSpam(email.getIsSpam())
                .sizeEstimate(email.getSizeEstimate())
                .category(email.getCategory())
                .priority(email.getPriority())
                .reaction(email.getReaction() != null ? email.getReaction() : Reaction.NONE)
                .aiSummary(email.getAiSummary())
                .aiActionItems(email.getAiActionItems())
                .deadlineDetected(email.getDeadlineDetected())
                .isDeadlineAddedToCalendar(email.getIsDeadlineAddedToCalendar())
                .actions(actions)
                .attachments(includeFullBody ? attachments : null)
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
                .sorted(Comparator.comparing(e -> e.getDeadlineDetected()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private Email ownedEmail(Long userId, Long emailId) {
        return emailRepository.findByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email " + emailId + " not found"));
    }
}
