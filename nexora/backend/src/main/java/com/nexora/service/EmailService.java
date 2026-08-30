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
import com.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Inbox reads/mutations + sync orchestration.
 * Heavy Gmail I/O and classify run via {@link GmailSyncService} / {@link PostSyncProcessingService}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmailRepository emailRepository;
    private final UserRepository userRepository;
    private final GmailSyncService gmailSyncService;
    private final EmailClassificationService classificationService;
    private final PostSyncProcessingService postSyncProcessingService;

    public Page<EmailResponse> getEmails(Long userId, String category, String priority,
                                          String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("receivedAt").descending());

        boolean hasSearch = search != null && !search.isBlank();
        EmailCategory categoryEnum = parseCategoryParam(category);
        Priority priorityEnum = parsePriorityParam(priority);

        Page<Email> emailPage;
        if (hasSearch && categoryEnum != null) {
            emailPage = emailRepository.searchInboxByUserIdAndCategory(
                    userId, search, categoryEnum, pageable);
        } else if (hasSearch) {
            emailPage = emailRepository.searchInboxByUserId(userId, search, pageable);
        } else if (categoryEnum != null && priorityEnum != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndCategoryAndPriorityOrderByReceivedAtDesc(
                    userId, categoryEnum, priorityEnum, pageable);
        } else if (categoryEnum != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
                    userId, categoryEnum, pageable);
        } else if (priorityEnum != null) {
            emailPage = emailRepository.findByUserIdAndInInboxTrueAndPriorityOrderByReceivedAtDesc(
                    userId, priorityEnum, pageable);
        } else {
            emailPage = emailRepository.findByUserIdAndInInboxTrueOrderByReceivedAtDesc(userId, pageable);
        }

        return emailPage.map(e -> toResponse(e, false));
    }

    public Page<EmailResponse> getDraftEmails(Long userId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("receivedAt").descending());
        Page<Email> emailPage = (search != null && !search.isBlank())
                ? emailRepository.searchDraftsByUserId(userId, search, pageable)
                : emailRepository.findByUserIdAndIsDraftTrueOrderByReceivedAtDesc(userId, pageable);
        return emailPage.map(e -> toResponse(e, false));
    }

    public Page<EmailResponse> getArchivedEmails(Long userId, String search, int page, int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("receivedAt").descending());
        Page<Email> emailPage = (search != null && !search.isBlank())
                ? emailRepository.searchArchivedByUserId(userId, search, pageable)
                : emailRepository.findByUserIdAndIsArchivedTrueOrderByReceivedAtDesc(userId, pageable);
        return emailPage.map(e -> toResponse(e, false));
    }

    public EmailResponse getEmailDetail(Long userId, Long emailId) {
        Email email = emailRepository.findDetailByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
        return toResponse(email, true);
    }

    public EmailResponse markRead(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (Boolean.TRUE.equals(email.getIsRead())) {
            return toResponse(email, false);
        }
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.markReadInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsRead(true);
        }
        return toResponse(emailRepository.save(email), false);
    }

    public EmailResponse markUnread(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.markUnreadInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsRead(false);
        }
        return toResponse(emailRepository.save(email), false);
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
        return toResponse(emailRepository.save(email), false);
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
        return toResponse(emailRepository.save(email), false);
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
        return toResponse(emailRepository.save(email), false);
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
        return toResponse(emailRepository.save(email), false);
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
        return toResponse(emailRepository.save(email), false);
    }

    public void updateReaction(Long userId, Long emailId, String reaction) {
        Email email = ownedEmail(userId, emailId);
        try {
            email.setReaction(Reaction.valueOf(reaction.toUpperCase()));
            emailRepository.save(email);
        } catch (IllegalArgumentException ex) {
            throw new NexoraException("Invalid reaction: " + reaction, 400);
        }
    }

    /**
     * Starts Gmail sync on a background executor and returns immediately.
     * Clients poll {@code /api/emails/sync-status} until mail appears.
     */
    public GmailSyncResponse syncInbox(Long userId) {
        if (gmailSyncService.hasActiveSync(userId)) {
            return new GmailSyncResponse(
                    "Sync already in progress — try again in a moment",
                    0, 0, 0, gmailSyncService.getLabelCounts(userId), "SKIPPED");
        }
        postSyncProcessingService.syncAndProcess(userId);
        return new GmailSyncResponse(
                "Gmail sync started — inbox will update shortly",
                0, 0, 0, Map.of(), "STARTED");
    }

    /** Runs sync on the calling thread (scheduler). Post-sync classify still goes async. */
    public GmailSyncResponse syncInboxBlocking(Long userId) {
        GmailSyncResponse response = gmailSyncService.syncInbox(userId);
        String mode = response.getSyncMode();
        if (mode != null && !"SKIPPED".equals(mode)) {
            postSyncProcessingService.process(userId, mode);
        }
        return response;
    }

    public Map<String, GmailLabelCountResponse> getGmailLabelCounts(Long userId) {
        return gmailSyncService.getLabelCounts(userId);
    }

    /** Queue classify + Gemini refine off the HTTP thread; return current group counts. */
    public Map<String, Object> classifyInbox(Long userId, boolean force) {
        postSyncProcessingService.classifyAndRefine(userId, force);
        return Map.of(
                "message", force
                        ? "Re-analysis started — groups refresh without clearing existing labels"
                        : "Classification started — groups update as mail is analyzed",
                "classified", 0,
                "groups", getCategoryCounts(userId),
                "forced", force,
                "started", true
        );
    }

    public SyncIntegrityResponse getSyncIntegrity(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NexoraException("User not found", 404));
        Map<String, GmailLabelCountResponse> labels = gmailSyncService.getLabelCounts(userId);

        long gmailInbox = labelTotal(labels.get("INBOX"));
        long gmailUnread = labelUnread(labels.get("INBOX"));
        long gmailDrafts = labelTotal(labels.get("DRAFT"));

        long localInbox = emailRepository.countByUserIdAndInInboxTrue(userId);
        long localDrafts = emailRepository.countByUserIdAndIsDraftTrue(userId);
        long localArchived = emailRepository.countByUserIdAndIsArchivedTrue(userId);
        long localTotal = emailRepository.countByUserId(userId);
        long localUnread = emailRepository.countInboxUnreadByUserId(userId);
        long unclassified = emailRepository.countByUserIdAndCategoryAndInInboxTrue(
                userId, EmailCategory.UNCATEGORIZED);

        Map<String, Long> gmailCounts = new LinkedHashMap<>();
        if (gmailInbox >= 0) gmailCounts.put("inboxTotal", gmailInbox);
        if (gmailUnread >= 0) gmailCounts.put("inboxUnread", gmailUnread);
        if (gmailDrafts >= 0) gmailCounts.put("drafts", gmailDrafts);
        long important = labelTotal(labels.get("IMPORTANT"));
        if (important >= 0) gmailCounts.put("important", important);
        long spam = labelTotal(labels.get("SPAM"));
        if (spam >= 0) gmailCounts.put("spam", spam);

        Map<String, Long> localCounts = new LinkedHashMap<>();
        localCounts.put("inboxTotal", localInbox);
        localCounts.put("inboxUnread", localUnread);
        localCounts.put("drafts", localDrafts);
        localCounts.put("archived", localArchived);
        localCounts.put("allStored", localTotal);

        boolean labelsCached = !labels.isEmpty();
        boolean inboxAligned = labelsCached && gmailInbox >= 0 && localInbox == gmailInbox;
        boolean draftsAligned = labelsCached && gmailDrafts >= 0 && localDrafts == gmailDrafts;
        boolean secondaryComplete = SyncStatusFlags.secondaryComplete(user.getGmailHistoryId());
        boolean syncInProgress = gmailSyncService.hasActiveSync(userId);

        List<String> notes = new ArrayList<>();
        if (user.getGmailAccessToken() == null) {
            notes.add("Gmail is not connected — no token on user.");
        }
        if (syncInProgress) {
            notes.add("Gmail sync is currently running.");
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
        boolean hasLastSynced = user.getLastSyncedAt() != null;
        if (SyncStatusFlags.enrichingInBackground(hasLastSynced, secondaryComplete, syncInProgress)) {
            notes.add("Background mailbox enrichment still running — drafts/archive and full bodies load next.");
        } else if (SyncStatusFlags.enrichmentIncompleteButIdle(secondaryComplete, syncInProgress, hasLastSynced)) {
            notes.add("Inbox is ready. Full-body enrichment may still catch up on next sync.");
        }
        if (localInbox == 0 && (gmailInbox > 0 || labels.isEmpty())) {
            notes.add("No inbox mail stored locally. Open Dashboard to trigger sync.");
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
        out.setSecondaryComplete(secondaryComplete);
        out.setSyncInProgress(syncInProgress);
        out.setNotes(notes);
        out.setSampleInbox(sample);
        return out;
    }

    public void classifyEmail(Long userId, Long emailId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NexoraException("User not found", 404));
        ownedEmail(userId, emailId);
        classificationService.classifyEmail(emailId, user);
    }

    public Map<String, Long> getCategoryCounts(Long userId) {
        List<Object[]> results = emailRepository.countByUserIdGroupByCategory(userId);
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : results) {
            if (row[0] == null) continue;
            counts.put(row[0].toString(), row[1] instanceof Number n ? n.longValue() : 0L);
        }
        return counts;
    }

    public List<SenderSummaryResponse> getSenderSummary(Long userId) {
        return emailRepository.countBySenderForUser(userId).stream()
                .map(row -> SenderSummaryResponse.builder()
                        .senderEmail(row[0] != null ? row[0].toString() : "")
                        .senderName(row[1] != null ? row[1].toString() : null)
                        .emailCount(row[2] instanceof Number n ? n.longValue() : 0L)
                        .latestReceivedAt(toLocalDateTime(row[3]))
                        .latestSubject(row[4] != null ? row[4].toString() : null)
                        .build())
                .collect(Collectors.toList());
    }

    public Page<EmailResponse> getEmailsBySender(Long userId, String senderEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, clampSize(size), Sort.by("receivedAt").descending());
        return emailRepository
                .findByUserIdAndSenderEmailOrderByReceivedAtDesc(userId, senderEmail, pageable)
                .map(e -> toResponse(e, false));
    }

    public EmailResponse toResponse(Email email, boolean includeFullBody) {
        List<EmailResponse.ActionItemDto> actions = List.of();
        if (includeFullBody && email.getActions() != null) {
            actions = email.getActions().stream().map(a -> EmailResponse.ActionItemDto.builder()
                    .id(a.getId())
                    .actionType(a.getActionType())
                    .actionDescription(a.getActionDescription())
                    .deadline(a.getDeadline())
                    .isCompleted(a.getIsCompleted())
                    .build()).collect(Collectors.toList());
        }

        List<EmailResponse.AttachmentDto> attachments = null;
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
                .attachments(attachments)
                .createdAt(email.getCreatedAt())
                .build();
    }

    public List<Map<String, Object>> getEmailVolume(Long userId, int days) {
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDateTime start = LocalDateTime.now().minusDays(safeDays - 1L)
                .withHour(0).withMinute(0).withSecond(0).withNano(0);
        List<LocalDateTime> dates = emailRepository.findReceivedAtByUserIdAndReceivedAtAfter(userId, start);

        Map<String, Long> grouped = new LinkedHashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (int i = safeDays - 1; i >= 0; i--) {
            grouped.put(java.time.LocalDate.now().minusDays(i).format(formatter), 0L);
        }
        for (LocalDateTime dt : dates) {
            String key = dt.toLocalDate().format(formatter);
            if (grouped.containsKey(key)) {
                grouped.put(key, grouped.get(key) + 1);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>(grouped.size());
        for (Map.Entry<String, Long> entry : grouped.entrySet()) {
            result.add(Map.of("date", entry.getKey(), "count", entry.getValue()));
        }
        return result;
    }

    public List<EmailResponse> getEmailThread(Long userId, String threadId) {
        return emailRepository.findByUserIdAndGmailThreadIdOrderByReceivedAtAsc(userId, threadId).stream()
                .map(e -> toResponse(e, false))
                .collect(Collectors.toList());
    }

    public String draftReply(Long userId, Long emailId, String style) {
        Email email = ownedEmail(userId, emailId);

        String body = email.getBodyFull() != null ? email.getBodyFull() : email.getBodySnippet();
        if (body == null) body = "";
        if (body.length() > 2000) body = body.substring(0, 2000);

        String systemPrompt = "Draft a " + style + " email reply. Return ONLY the reply body text. "
                + "No subject line. No 'Dear...' unless formal. No sign-off unless formal. No HTML tags.";

        String userMessage = "Original email from "
                + (email.getSenderName() != null ? email.getSenderName() : email.getSenderEmail())
                + ":\nSubject: " + (email.getSubject() != null ? email.getSubject() : "")
                + "\n\n" + body;

        String draft = classificationService.generateBrainAnswer(systemPrompt, userMessage);
        if (draft != null && !draft.isBlank()) {
            return draft.trim();
        }

        return "Hi " + (email.getSenderName() != null ? email.getSenderName() : "there") + ",\n\n"
                + "Thanks for your email regarding \""
                + (email.getSubject() != null ? email.getSubject() : "") + "\".\n"
                + "I'll review this and get back to you shortly.\n\n"
                + "Regards,\n[Your Name]";
    }

    public List<EmailResponse> getPriorityEmails(Long userId, int limit) {
        int capped = Math.max(1, Math.min(limit, 80));
        List<EmailResponse> out = new ArrayList<>(capped);
        for (Priority p : List.of(Priority.HIGH, Priority.MEDIUM, Priority.LOW)) {
            if (out.size() >= capped) break;
            int remaining = capped - out.size();
            List<Email> band = emailRepository
                    .findByUserIdAndInInboxTrueAndPriorityOrderByReceivedAtDesc(
                            userId, p, PageRequest.of(0, remaining))
                    .getContent();
            for (Email e : band) {
                if (out.size() >= capped) break;
                out.add(toResponse(e, false));
            }
        }
        return out;
    }

    public EmailResponse flagAsImportant(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.markImportantInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsImportant(true);
        }
        email.setReaction(Reaction.IMPORTANT);
        email.setPriority(Priority.HIGH);
        return toResponse(emailRepository.save(email), false);
    }

    public EmailResponse unflagAsImportant(Long userId, Long emailId) {
        Email email = ownedEmail(userId, emailId);
        if (email.getGmailMessageId() != null) {
            var gmailMsg = gmailSyncService.unmarkImportantInGmail(userId, email.getGmailMessageId());
            gmailSyncService.applyGmailMessageLabels(email, gmailMsg);
        } else {
            email.setIsImportant(false);
        }
        if (email.getReaction() == Reaction.IMPORTANT) {
            email.setReaction(Reaction.NONE);
        }
        return toResponse(emailRepository.save(email), false);
    }

    public List<EmailResponse> getSuggestedPriorityEmails(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime horizon = now.plusDays(7);
        List<Email> candidates = new ArrayList<>();
        candidates.addAll(emailRepository
                .findByUserIdAndInInboxTrueAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Priority.MEDIUM, PageRequest.of(0, 50)));
        candidates.addAll(emailRepository
                .findByUserIdAndInInboxTrueAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Priority.LOW, PageRequest.of(0, 50)));

        return candidates.stream()
                .filter(e -> e.getDeadlineDetected() != null)
                .filter(e -> !e.getDeadlineDetected().isBefore(now)
                        && e.getDeadlineDetected().isBefore(horizon))
                .sorted(Comparator.comparing(Email::getDeadlineDetected,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .limit(10)
                .map(e -> toResponse(e, false))
                .collect(Collectors.toList());
    }

    private Email ownedEmail(Long userId, Long emailId) {
        return emailRepository.findOwnedByIdAndUserId(emailId, userId)
                .orElseThrow(() -> new NexoraException("Email not found", 404));
    }

    private static int clampSize(int size) {
        if (size < 1) return 20;
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static long labelTotal(GmailLabelCountResponse label) {
        return label != null && label.getMessagesTotal() != null ? label.getMessagesTotal() : -1;
    }

    private static long labelUnread(GmailLabelCountResponse label) {
        return label != null && label.getMessagesUnread() != null ? label.getMessagesUnread() : -1;
    }

    private EmailCategory parseCategoryParam(String category) {
        if (category == null || category.isBlank()) return null;
        try {
            return EmailCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NexoraException("Invalid category: " + category, 400);
        }
    }

    private Priority parsePriorityParam(String priority) {
        if (priority == null || priority.isBlank()) return null;
        try {
            return Priority.valueOf(priority.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NexoraException("Invalid priority: " + priority, 400);
        }
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime ldt) return ldt;
        if (value instanceof java.sql.Timestamp ts) return ts.toLocalDateTime();
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneId.systemDefault());
        }
        return null;
    }
}
