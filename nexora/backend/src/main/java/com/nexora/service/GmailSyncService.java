package com.nexora.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;
import com.nexora.config.GmailConfig;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.dto.response.GmailSyncResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.EmailAttachment;
import com.nexora.model.User;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import com.nexora.security.TokenEncryptor;
import com.nexora.util.HtmlSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailSyncService {

    private final GmailConfig gmailConfig;
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final TokenEncryptor tokenEncryptor;
    private final ObjectMapper objectMapper;

    private final Set<Long> activeSyncs = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static final int PAGE_SIZE = 100;
    private static final int MAX_RETRIES = 3;
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");

    /**
     * Unified sync entry: prefers incremental when a historyId is stored;
     * falls back to full sync on first sync or invalid/expired history.
     */
    public GmailSyncResponse syncInbox(Long userId) {
        if (!activeSyncs.add(userId)) {
            log.info("Gmail sync already in progress for user {} — skipping concurrent request", userId);
            return emptyResponse("Sync already in progress", null);
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NexoraException("User not found", 404));

            if (user.getGmailAccessToken() == null) {
                log.warn("User {} has no Gmail access token — skipping sync", userId);
                return emptyResponse("No Gmail connection — connect your account first", null);
            }

            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);

            if (user.getGmailHistoryId() != null && !user.getGmailHistoryId().isBlank()) {
                try {
                    return syncIncremental(gmail, user);
                } catch (HistoryOutOfDateException e) {
                    log.warn("History ID invalid for user {} — falling back to full sync: {}",
                            userId, e.getMessage());
                    return fullSync(gmail, user);
                }
            }

            return fullSync(gmail, user);

        } catch (GeneralSecurityException | IOException e) {
            log.error("Gmail sync failed for user {}: {}", userId, e.getMessage());
            throw new NexoraException("Gmail sync failed: " + e.getMessage(), 400);
        } finally {
            activeSyncs.remove(userId);
        }
    }

    private GmailSyncResponse fullSync(Gmail gmail, User user) throws IOException {
        Long userId = user.getId();

        Map<String, GmailLabelCountResponse> labelCounts = fetchAndCacheLabelCounts(gmail, user);

        List<Message> inboxMessages = listMessagesByLabel(gmail, "INBOX", Integer.MAX_VALUE);
        log.info("Fetched {} INBOX message refs for user {}", inboxMessages.size(), userId);

        int newCount = 0;
        int updatedCount = 0;

        Set<String> seenInboxIds = new HashSet<>();
        int[] inboxStats = upsertMessageBatch(gmail, user, inboxMessages, MailboxKind.INBOX, seenInboxIds);
        newCount += inboxStats[0];
        updatedCount += inboxStats[1];

        updatedCount += markRemovedFromInbox(userId, seenInboxIds);

        List<Message> draftMessages = listMessagesByLabel(gmail, "DRAFT", Integer.MAX_VALUE);
        log.info("Fetched {} DRAFT message refs for user {}", draftMessages.size(), userId);
        int[] draftStats = upsertMessageBatch(gmail, user, draftMessages, MailboxKind.DRAFT, null);
        newCount += draftStats[0];
        updatedCount += draftStats[1];

        List<Message> archivedMessages = listMessagesByQuery(
                gmail, "-in:inbox -in:trash -in:spam -in:drafts", 300);
        log.info("Fetched {} archived message refs for user {}", archivedMessages.size(), userId);
        int[] archiveStats = upsertMessageBatch(gmail, user, archivedMessages, MailboxKind.ARCHIVE, null);
        newCount += archiveStats[0];
        updatedCount += archiveStats[1];

        storeProfileHistoryId(gmail, user);
        user.setLastSyncedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Full sync complete for user {}: {} new, {} updated (inbox={}, drafts={}, archive={})",
                userId, newCount, updatedCount,
                inboxMessages.size(), draftMessages.size(), archivedMessages.size());

        return new GmailSyncResponse(
                "Full sync completed successfully",
                newCount,
                updatedCount,
                inboxMessages.size(),
                labelCounts,
                "FULL"
        );
    }

    private GmailSyncResponse syncIncremental(Gmail gmail, User user)
            throws IOException, HistoryOutOfDateException {
        Long userId = user.getId();
        BigInteger startHistoryId;
        try {
            startHistoryId = new BigInteger(user.getGmailHistoryId().trim());
        } catch (NumberFormatException e) {
            throw new HistoryOutOfDateException("Stored historyId is not numeric");
        }

        Map<String, GmailLabelCountResponse> labelCounts = fetchAndCacheLabelCounts(gmail, user);

        int newCount = 0;
        int updatedCount = 0;
        int deletedCount = 0;
        BigInteger latestHistoryId = null;
        String pageToken = null;

        do {
            ListHistoryResponse response;
            try {
                var request = gmail.users().history().list("me")
                        .setStartHistoryId(startHistoryId)
                        .setMaxResults((long) PAGE_SIZE);
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }
                response = request.execute();
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 404) {
                    throw new HistoryOutOfDateException("history.list returned 404");
                }
                throw e;
            }

            if (response.getHistoryId() != null) {
                latestHistoryId = response.getHistoryId();
            }

            List<History> histories = response.getHistory();
            if (histories != null) {
                for (History history : histories) {
                    if (history.getId() != null) {
                        latestHistoryId = history.getId();
                    }
                    int[] counts = processHistoryRecord(gmail, user, history);
                    newCount += counts[0];
                    updatedCount += counts[1];
                    deletedCount += counts[2];
                }
            }

            pageToken = response.getNextPageToken();
        } while (pageToken != null);

        if (latestHistoryId != null) {
            user.setGmailHistoryId(String.valueOf(latestHistoryId));
        }
        user.setLastSyncedAt(LocalDateTime.now());
        userRepository.save(user);

        log.info("Incremental sync for user {}: {} new, {} updated, {} deleted",
                userId, newCount, updatedCount, deletedCount);

        return new GmailSyncResponse(
                "Incremental sync completed successfully",
                newCount,
                updatedCount,
                newCount + updatedCount,
                labelCounts,
                "INCREMENTAL"
        );
    }

    /** @return int[]{newCount, updatedCount, deletedCount} */
    private int[] processHistoryRecord(Gmail gmail, User user, History history) {
        int newCount = 0;
        int updatedCount = 0;
        int deletedCount = 0;
        Long userId = user.getId();
        Set<String> processedAdds = new HashSet<>();
        Set<String> processedLabelTouch = new HashSet<>();

        if (history.getMessagesAdded() != null) {
            for (HistoryMessageAdded added : history.getMessagesAdded()) {
                if (added.getMessage() == null || added.getMessage().getId() == null) continue;
                String msgId = added.getMessage().getId();
                if (!processedAdds.add(msgId)) continue;
                try {
                    Message full = fetchWithRetry(gmail, msgId);
                    if (full == null) continue;
                    boolean created = upsertFullMessage(user, full, null);
                    if (created) newCount++;
                    else updatedCount++;
                } catch (Exception e) {
                    log.error("Failed to process messagesAdded {}: {}", msgId, e.getMessage());
                }
            }
        }

        if (history.getMessagesDeleted() != null) {
            for (HistoryMessageDeleted deleted : history.getMessagesDeleted()) {
                if (deleted.getMessage() == null || deleted.getMessage().getId() == null) continue;
                String msgId = deleted.getMessage().getId();
                Optional<Email> existing = emailRepository.findByUserIdAndGmailMessageId(userId, msgId);
                if (existing.isPresent()) {
                    emailRepository.delete(existing.get());
                    deletedCount++;
                }
            }
        }

        if (history.getLabelsAdded() != null) {
            for (HistoryLabelAdded labelsAdded : history.getLabelsAdded()) {
                if (labelsAdded.getMessage() == null || labelsAdded.getMessage().getId() == null) continue;
                String msgId = labelsAdded.getMessage().getId();
                if (!processedLabelTouch.add("add:" + msgId) && processedAdds.contains(msgId)) continue;
                updatedCount += applyLabelHistoryChange(gmail, user, msgId);
            }
        }

        if (history.getLabelsRemoved() != null) {
            for (HistoryLabelRemoved labelsRemoved : history.getLabelsRemoved()) {
                if (labelsRemoved.getMessage() == null || labelsRemoved.getMessage().getId() == null) continue;
                String msgId = labelsRemoved.getMessage().getId();
                if (!processedLabelTouch.add("rem:" + msgId) && processedAdds.contains(msgId)) continue;
                updatedCount += applyLabelHistoryChange(gmail, user, msgId);
            }
        }

        return new int[]{newCount, updatedCount, deletedCount};
    }

    /**
     * Prefer authoritative FULL fetch for label changes.
     * Do not invent full mailbox state from partial history stub labels.
     * @return 1 if updated, 0 otherwise
     */
    private int applyLabelHistoryChange(Gmail gmail, User user, String messageId) {
        Optional<Email> existingOpt = emailRepository.findByUserIdAndGmailMessageId(user.getId(), messageId);
        Message full = fetchWithRetry(gmail, messageId);
        if (full != null) {
            if (existingOpt.isEmpty()) {
                upsertFullMessage(user, full, null);
            } else {
                Email email = existingOpt.get();
                List<String> labelIds = full.getLabelIds() != null ? full.getLabelIds() : List.of();
                applyLabelFields(email, labelIds);
                email.setGmailLabelIds(labelsToJson(labelIds));
                emailRepository.save(email);
            }
            return 1;
        }
        // No FULL message — refuse to invent state from partial history stubs
        log.warn("Could not fetch message {} for label history; skipping local update", messageId);
        return 0;
    }

    private void storeProfileHistoryId(Gmail gmail, User user) {
        try {
            Profile profile = gmail.users().getProfile("me").execute();
            if (profile.getHistoryId() != null) {
                user.setGmailHistoryId(String.valueOf(profile.getHistoryId()));
            }
        } catch (Exception e) {
            log.warn("Could not fetch Gmail profile historyId for user {}: {}", user.getId(), e.getMessage());
        }
    }

    public Map<String, GmailLabelCountResponse> getLabelCounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NexoraException("User not found", 404));
        return parseLabelCountsJson(user.getGmailLabelCounts());
    }

    public Map<String, GmailLabelCountResponse> refreshLabelCounts(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NexoraException("User not found", 404));
        if (user.getGmailAccessToken() == null) {
            return Map.of();
        }
        try {
            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);
            return fetchAndCacheLabelCounts(gmail, user);
        } catch (Exception e) {
            log.error("Failed to refresh label counts for user {}: {}", userId, e.getMessage());
            throw new NexoraException("Failed to fetch Gmail label counts: " + e.getMessage(), 400);
        }
    }

    public long getInboxUnreadCount(Long userId) {
        Map<String, GmailLabelCountResponse> counts = getLabelCounts(userId);
        GmailLabelCountResponse inbox = counts.get("INBOX");
        if (inbox != null && inbox.getMessagesUnread() != null) {
            return inbox.getMessagesUnread();
        }
        return emailRepository.countInboxUnreadByUserId(userId);
    }

    // ─── Gmail API helpers ───────────────────────────────────────────────────

    private Map<String, GmailLabelCountResponse> fetchAndCacheLabelCounts(Gmail gmail, User user)
            throws IOException {
        ListLabelsResponse response = gmail.users().labels().list("me").execute();
        Map<String, GmailLabelCountResponse> counts = new LinkedHashMap<>();

        if (response.getLabels() != null) {
            for (Label label : response.getLabels()) {
                if (label.getId() == null) continue;
                counts.put(label.getId(), new GmailLabelCountResponse(
                        label.getId(),
                        label.getName(),
                        label.getType(),
                        toLong(label.getMessagesTotal()),
                        toLong(label.getMessagesUnread()),
                        toLong(label.getThreadsTotal()),
                        toLong(label.getThreadsUnread())
                ));
            }
        }

        try {
            user.setGmailLabelCounts(objectMapper.writeValueAsString(counts));
            userRepository.save(user);
        } catch (Exception e) {
            log.warn("Could not persist label counts for user {}: {}", user.getId(), e.getMessage());
        }

        return counts;
    }

    private enum MailboxKind { INBOX, DRAFT, ARCHIVE }

    /** @return int[]{newCount, updatedCount} */
    private int[] upsertMessageBatch(Gmail gmail, User user, List<Message> stubs,
                                     MailboxKind kind, Set<String> collectIds) {
        int newCount = 0;
        int updatedCount = 0;
        Set<String> seen = new HashSet<>();

        for (Message stub : stubs) {
            if (stub.getId() == null || !seen.add(stub.getId())) continue;
            if (collectIds != null) collectIds.add(stub.getId());

            try {
                var existingOpt = emailRepository.findByUserIdAndGmailMessageId(user.getId(), stub.getId());
                if (existingOpt.isPresent()) {
                    Email existing = existingOpt.get();
                    Message full = fetchWithRetry(gmail, stub.getId());
                    if (full != null) {
                        applyFullMessageUpdate(existing, full, kind);
                        emailRepository.save(existing);
                        updatedCount++;
                    } else {
                        applyMailboxKind(existing, kind);
                        applyStubMetadata(existing, stub);
                        emailRepository.save(existing);
                        updatedCount++;
                    }
                    continue;
                }

                Message fullMessage = fetchWithRetry(gmail, stub.getId());
                if (fullMessage == null) continue;

                Email email = parseMessage(fullMessage, user);
                applyMailboxKind(email, kind);
                emailRepository.save(email);
                newCount++;
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Constraint violation saving message {}, skipping: {}", stub.getId(), e.getMessage());
            } catch (Exception e) {
                log.error("Failed to sync message {}: {}", stub.getId(), e.getMessage());
            }
        }
        return new int[]{newCount, updatedCount};
    }

    /**
     * Upsert from a FULL Gmail message. Returns true if newly created.
     */
    private boolean upsertFullMessage(User user, Message full, MailboxKind kind) {
        Optional<Email> existingOpt =
                emailRepository.findByUserIdAndGmailMessageId(user.getId(), full.getId());
        if (existingOpt.isPresent()) {
            Email existing = existingOpt.get();
            applyFullMessageUpdate(existing, full, kind);
            emailRepository.save(existing);
            return false;
        }
        Email email = parseMessage(full, user);
        if (kind != null) {
            applyMailboxKind(email, kind);
        }
        emailRepository.save(email);
        return true;
    }

    private void applyFullMessageUpdate(Email email, Message full, MailboxKind kind) {
        List<String> labelIds = full.getLabelIds() != null ? full.getLabelIds() : List.of();
        applyLabelFields(email, labelIds);
        email.setGmailLabelIds(labelsToJson(labelIds));
        if (full.getSnippet() != null) {
            email.setBodySnippet(truncate(full.getSnippet(), 500));
        }
        if (full.getThreadId() != null) {
            email.setGmailThreadId(full.getThreadId());
        }
        if (full.getSizeEstimate() != null) {
            email.setSizeEstimate(full.getSizeEstimate().longValue());
        }

        MessagePart payload = full.getPayload();
        if (payload != null) {
            List<MessagePartHeader> headers = payload.getHeaders() != null
                    ? payload.getHeaders() : List.of();
            String subject = getHeader(headers, "Subject");
            if (!subject.isBlank()) email.setSubject(subject);
            String to = getHeader(headers, "To");
            if (!to.isBlank()) email.setRecipientTo(to);
            String cc = getHeader(headers, "Cc");
            if (!cc.isBlank()) email.setRecipientCc(cc);
            String bcc = getHeader(headers, "Bcc");
            if (!bcc.isBlank()) email.setRecipientBcc(bcc);
            String replyTo = getHeader(headers, "Reply-To");
            if (!replyTo.isBlank()) email.setReplyTo(replyTo);

            BodyContent body = extractBodyContent(payload);
            email.setBodyHtml(body.html);
            email.setBodyFull(body.plain);

            List<EmailAttachment> attachments = collectAttachments(payload);
            replaceAttachments(email, attachments);
            email.setHasAttachments(!attachments.isEmpty());
        }

        if (kind != null) {
            applyMailboxKind(email, kind);
        }
    }

    private void applyMailboxKind(Email email, MailboxKind kind) {
        switch (kind) {
            case INBOX -> {
                email.setInInbox(true);
                email.setIsDraft(false);
                email.setIsArchived(false);
            }
            case DRAFT -> {
                email.setInInbox(false);
                email.setIsDraft(true);
                email.setIsArchived(false);
            }
            case ARCHIVE -> {
                email.setInInbox(false);
                email.setIsDraft(false);
                email.setIsArchived(true);
            }
        }
    }

    private List<Message> listMessagesByLabel(Gmail gmail, String labelId, int maxTotal) throws IOException {
        List<Message> all = new ArrayList<>();
        String nextPageToken = null;

        do {
            var request = gmail.users().messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList(labelId))
                    .setMaxResults((long) PAGE_SIZE);
            if (nextPageToken != null) {
                request.setPageToken(nextPageToken);
            }

            ListMessagesResponse response = request.execute();
            if (response.getMessages() != null) {
                all.addAll(response.getMessages());
            }
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null && all.size() < maxTotal);

        if (all.size() > maxTotal) {
            return all.subList(0, maxTotal);
        }
        return all;
    }

    private List<Message> listMessagesByQuery(Gmail gmail, String query, int maxTotal) throws IOException {
        List<Message> all = new ArrayList<>();
        String nextPageToken = null;

        do {
            long pageSize = Math.min(PAGE_SIZE, maxTotal - all.size());
            if (pageSize <= 0) break;

            var request = gmail.users().messages()
                    .list("me")
                    .setQ(query)
                    .setMaxResults(pageSize);
            if (nextPageToken != null) {
                request.setPageToken(nextPageToken);
            }

            ListMessagesResponse response = request.execute();
            if (response.getMessages() != null) {
                all.addAll(response.getMessages());
            }
            nextPageToken = response.getNextPageToken();
        } while (nextPageToken != null && all.size() < maxTotal);

        return all;
    }

    private int markRemovedFromInbox(Long userId, Set<String> currentInboxIds) {
        List<Email> candidates = emailRepository.findByUserIdAndInInboxTrue(userId);
        int changed = 0;
        for (Email email : candidates) {
            if (email.getGmailMessageId() != null && !currentInboxIds.contains(email.getGmailMessageId())) {
                if (Boolean.TRUE.equals(email.getIsDraft())) continue;
                email.setInInbox(false);
                email.setIsArchived(true);
                emailRepository.save(email);
                changed++;
            }
        }
        List<Email> legacy = emailRepository.findByUserIdWithNullInInbox(userId);
        for (Email email : legacy) {
            if (email.getGmailMessageId() != null && !currentInboxIds.contains(email.getGmailMessageId())) {
                email.setInInbox(false);
                if (!Boolean.TRUE.equals(email.getIsDraft())) {
                    email.setIsArchived(true);
                }
                emailRepository.save(email);
                changed++;
            } else if (email.getGmailMessageId() != null && currentInboxIds.contains(email.getGmailMessageId())) {
                email.setInInbox(true);
                email.setIsArchived(false);
                email.setIsDraft(false);
                emailRepository.save(email);
                changed++;
            }
        }
        return changed;
    }

    private boolean applyStubMetadata(Email email, Message stub) {
        List<String> labelIds = stub.getLabelIds() != null ? stub.getLabelIds() : List.of();
        boolean changed = applyLabelFields(email, labelIds);
        if (!labelIds.isEmpty()) {
            String newLabelsJson = labelsToJson(labelIds);
            if (!Objects.equals(email.getGmailLabelIds(), newLabelsJson)) {
                email.setGmailLabelIds(newLabelsJson);
                changed = true;
            }
        }
        return changed;
    }

    private boolean applyLabelFields(Email email, List<String> labelIds) {
        if (labelIds == null || labelIds.isEmpty()) return false;

        boolean changed = false;
        boolean isRead = !labelIds.contains("UNREAD");
        boolean isStarred = labelIds.contains("STARRED");
        boolean isImportant = labelIds.contains("IMPORTANT");
        boolean isDraft = labelIds.contains("DRAFT");
        boolean isTrash = labelIds.contains("TRASH");
        boolean isSpam = labelIds.contains("SPAM");
        boolean inInbox = labelIds.contains("INBOX") && !isTrash && !isSpam;
        boolean isArchived = !inInbox && !isDraft && !isTrash && !isSpam;

        if (!Objects.equals(email.getIsRead(), isRead)) { email.setIsRead(isRead); changed = true; }
        if (!Objects.equals(email.getIsStarred(), isStarred)) { email.setIsStarred(isStarred); changed = true; }
        if (!Objects.equals(email.getIsImportant(), isImportant)) { email.setIsImportant(isImportant); changed = true; }
        if (!Objects.equals(email.getInInbox(), inInbox)) { email.setInInbox(inInbox); changed = true; }
        if (!Objects.equals(email.getIsDraft(), isDraft)) { email.setIsDraft(isDraft); changed = true; }
        if (!Objects.equals(email.getIsArchived(), isArchived)) { email.setIsArchived(isArchived); changed = true; }
        if (!Objects.equals(email.getIsTrash(), isTrash)) { email.setIsTrash(isTrash); changed = true; }
        if (!Objects.equals(email.getIsSpam(), isSpam)) { email.setIsSpam(isSpam); changed = true; }
        return changed;
    }

    private Gmail buildGmailClient(User user) throws GeneralSecurityException, IOException {
        String accessToken = tokenEncryptor.decrypt(user.getGmailAccessToken());
        String refreshToken = user.getGmailRefreshToken() != null
                ? tokenEncryptor.decrypt(user.getGmailRefreshToken()) : null;
        Date expiry = user.getTokenExpiry() != null
                ? Date.from(user.getTokenExpiry().atZone(ZoneId.systemDefault()).toInstant())
                : new Date();
        return gmailConfig.buildGmailService(accessToken, refreshToken, expiry);
    }

    private void ensureFreshToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        if (user.getTokenExpiry() == null || user.getTokenExpiry().isBefore(now.plusMinutes(5))) {
            log.info("Access token for user {} is expired or close to expiry — refreshing...", user.getId());
            refreshUserAccessToken(user);
        }
    }

    private void refreshUserAccessToken(User user) {
        if (user.getGmailRefreshToken() == null) {
            throw new NexoraException("No refresh token available to refresh access token", 401);
        }
        String refreshToken = tokenEncryptor.decrypt(user.getGmailRefreshToken());

        RestTemplate restTemplate = new RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED);

        org.springframework.util.MultiValueMap<String, String> body = new org.springframework.util.LinkedMultiValueMap<>();
        body.add("client_id", gmailConfig.getClientId());
        body.add("client_secret", gmailConfig.getClientSecret());
        body.add("refresh_token", refreshToken);
        body.add("grant_type", "refresh_token");

        org.springframework.http.HttpEntity<org.springframework.util.MultiValueMap<String, String>> request =
                new org.springframework.http.HttpEntity<>(body, headers);

        try {
            @SuppressWarnings("rawtypes")
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token", request, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("access_token")) {
                String newAccessToken = (String) responseBody.get("access_token");
                Number expiresIn = (Number) responseBody.get("expires_in");
                long seconds = expiresIn != null ? expiresIn.longValue() : 3600L;

                user.setGmailAccessToken(tokenEncryptor.encrypt(newAccessToken));
                user.setTokenExpiry(LocalDateTime.now().plusSeconds(seconds));
                userRepository.save(user);
                log.info("Successfully refreshed access token for user {}", user.getId());
            } else {
                throw new NexoraException("Google token endpoint response did not contain access_token", 401);
            }
        } catch (Exception e) {
            log.error("Error refreshing token from Google API: {}", e.getMessage());
            throw new NexoraException("Failed to refresh Gmail access token: " + e.getMessage(), 401);
        }
    }

    private Message fetchWithRetry(Gmail gmail, String messageId) {
        int attempt = 0;
        while (attempt < MAX_RETRIES) {
            try {
                return gmail.users().messages().get("me", messageId)
                        .setFormat("FULL").execute();
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 403) {
                    log.warn("Gmail API rejected FULL format for message {}. Retrying in METADATA format...", messageId);
                    try {
                        return gmail.users().messages().get("me", messageId)
                                .setFormat("METADATA")
                                .setMetadataHeaders(Arrays.asList(
                                        "Subject", "From", "To", "Cc", "Bcc", "Reply-To", "Date", "Message-ID"))
                                .execute();
                    } catch (IOException ex) {
                        log.error("Failed to fetch message in METADATA format: {}", ex.getMessage());
                        return null;
                    }
                } else if (e.getStatusCode() == 429) {
                    attempt++;
                    long waitMs = (long) Math.pow(2, attempt) * 1000;
                    log.warn("Rate limited — waiting {}ms before retry {}", waitMs, attempt);
                    sleep(waitMs);
                } else {
                    log.error("Gmail API error for message {}: {}", messageId, e.getMessage());
                    return null;
                }
            } catch (IOException e) {
                log.error("IO error fetching message {}: {}", messageId, e.getMessage());
                return null;
            }
        }
        return null;
    }

    private Email parseMessage(Message message, User user) {
        MessagePart payload = message.getPayload();
        List<MessagePartHeader> headers = payload != null && payload.getHeaders() != null
                ? payload.getHeaders() : new ArrayList<>();

        String subject = getHeader(headers, "Subject");
        String from = getHeader(headers, "From");
        String to = getHeader(headers, "To");
        String cc = getHeader(headers, "Cc");
        String bcc = getHeader(headers, "Bcc");
        String replyTo = getHeader(headers, "Reply-To");

        String[] fromParts = parseFrom(from);
        String senderName = fromParts[0];
        String senderEmail = fromParts[1] != null ? fromParts[1].trim() : "";
        if (senderEmail.isBlank()) {
            senderEmail = user.getEmail() != null ? user.getEmail() : "unknown@gmail.com";
            if (senderName == null || senderName.isBlank()) {
                senderName = user.getName() != null ? user.getName() : "Me";
            }
        }
        LocalDateTime received = parseDate(message.getInternalDate());
        List<String> labelIds = message.getLabelIds() != null ? message.getLabelIds() : List.of();

        BodyContent body = payload != null ? extractBodyContent(payload) : BodyContent.empty();
        List<EmailAttachment> attachments = payload != null ? collectAttachments(payload) : List.of();
        String snippet = message.getSnippet();

        Email email = Email.builder()
                .user(user)
                .gmailMessageId(message.getId())
                .gmailThreadId(message.getThreadId())
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodySnippet(truncate(snippet, 500))
                .bodyFull(body.plain)
                .bodyHtml(body.html)
                .receivedAt(received)
                .hasAttachments(!attachments.isEmpty())
                .recipientTo(to)
                .recipientCc(cc)
                .recipientBcc(bcc)
                .replyTo(replyTo.isBlank() ? null : replyTo)
                .sizeEstimate(message.getSizeEstimate() != null ? message.getSizeEstimate().longValue() : null)
                .gmailLabelIds(labelsToJson(labelIds))
                .category(Email.EmailCategory.UNCATEGORIZED)
                .priority(Email.Priority.MEDIUM)
                .attachments(new ArrayList<>())
                .build();

        replaceAttachments(email, attachments);
        applyLabelFields(email, labelIds);
        return email;
    }

    private void replaceAttachments(Email email, List<EmailAttachment> attachments) {
        if (email.getAttachments() == null) {
            email.setAttachments(new ArrayList<>());
        }
        email.getAttachments().clear();
        for (EmailAttachment att : attachments) {
            att.setEmail(email);
            email.getAttachments().add(att);
        }
    }

    private static final class BodyContent {
        final String plain;
        final String html;

        BodyContent(String plain, String html) {
            this.plain = plain;
            this.html = html;
        }

        static BodyContent empty() {
            return new BodyContent("", null);
        }
    }

    /**
     * Recursively extract text/plain and text/html.
     * bodyHtml = sanitized HTML; bodyFull = plain or htmlToPlainText fallback.
     */
    private BodyContent extractBodyContent(MessagePart payload) {
        String plain = extractPart(payload, "text/plain");
        String rawHtml = extractPart(payload, "text/html");
        String sanitizedHtml = rawHtml != null && !rawHtml.isBlank()
                ? HtmlSanitizer.sanitize(rawHtml) : null;

        String bodyFull;
        if (plain != null && !plain.isBlank()) {
            bodyFull = plain;
        } else if (rawHtml != null && !rawHtml.isBlank()) {
            String fromHtml = HtmlSanitizer.htmlToPlainText(rawHtml);
            bodyFull = fromHtml != null && !fromHtml.isBlank()
                    ? fromHtml
                    : HTML_TAGS.matcher(rawHtml).replaceAll(" ").trim();
        } else {
            bodyFull = "";
        }

        return new BodyContent(bodyFull, sanitizedHtml);
    }

    private List<EmailAttachment> collectAttachments(MessagePart payload) {
        List<EmailAttachment> out = new ArrayList<>();
        collectAttachmentsRecursive(payload, out);
        return out;
    }

    private void collectAttachmentsRecursive(MessagePart part, List<EmailAttachment> out) {
        if (part == null) return;

        String filename = part.getFilename();
        boolean hasFilename = filename != null && !filename.isEmpty();
        MessagePartBody body = part.getBody();
        String attachmentId = body != null ? body.getAttachmentId() : null;

        if (hasFilename || (attachmentId != null && !attachmentId.isBlank()
                && !isTextPart(part.getMimeType()))) {
            List<MessagePartHeader> partHeaders = part.getHeaders() != null ? part.getHeaders() : List.of();
            String contentId = stripContentIdBrackets(getHeader(partHeaders, "Content-ID"));
            String disposition = getHeader(partHeaders, "Content-Disposition");
            boolean inline = (disposition != null && disposition.toLowerCase(Locale.ROOT).contains("inline"))
                    || (contentId != null && !contentId.isBlank());

            Long sizeBytes = null;
            if (body != null && body.getSize() != null) {
                sizeBytes = body.getSize().longValue();
            }

            out.add(EmailAttachment.builder()
                    .gmailAttachmentId(attachmentId)
                    .filename(hasFilename ? filename : "attachment")
                    .mimeType(part.getMimeType())
                    .sizeBytes(sizeBytes)
                    .contentId(contentId)
                    .isInline(inline)
                    .build());
        }

        if (part.getParts() != null) {
            for (MessagePart child : part.getParts()) {
                collectAttachmentsRecursive(child, out);
            }
        }
    }

    private boolean isTextPart(String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/plain") || lower.startsWith("text/html");
    }

    private String stripContentIdBrackets(String contentId) {
        if (contentId == null || contentId.isBlank()) return null;
        String trimmed = contentId.trim();
        if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length() > 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String labelsToJson(List<String> labelIds) {
        try {
            return objectMapper.writeValueAsString(labelIds);
        } catch (Exception e) {
            return "[]";
        }
    }

    private Map<String, GmailLabelCountResponse> parseLabelCountsJson(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, GmailLabelCountResponse>>() {});
        } catch (Exception e) {
            log.warn("Could not parse cached label counts: {}", e.getMessage());
            return Map.of();
        }
    }

    private GmailSyncResponse emptyResponse(String message, String syncMode) {
        return new GmailSyncResponse(message, 0, 0, 0, Map.of(), syncMode);
    }

    // ─── Parsing helpers ─────────────────────────────────────────────────────

    private String getHeader(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(h -> h != null && h.getName() != null && h.getName().equalsIgnoreCase(name))
                .map(h -> h.getValue() != null ? h.getValue() : "")
                .findFirst()
                .orElse("");
    }

    private String[] parseFrom(String from) {
        if (from.contains("<")) {
            String name = from.substring(0, from.indexOf("<")).trim().replaceAll("\"", "");
            String email = from.substring(from.indexOf("<") + 1, from.indexOf(">")).trim();
            return new String[]{name, email};
        }
        return new String[]{"", from.trim()};
    }

    private LocalDateTime parseDate(Long internalDate) {
        if (internalDate == null) return LocalDateTime.now();
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(internalDate), ZoneId.systemDefault());
    }

    private String truncate(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String extractPart(MessagePart payload, String mimeType) {
        if (payload == null) return null;

        if (mimeType.equalsIgnoreCase(payload.getMimeType()) && payload.getBody() != null) {
            String data = payload.getBody().getData();
            if (data != null) return decode(data);
        }

        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                String result = extractPart(part, mimeType);
                if (result != null) return result;
            }
        }
        return null;
    }

    private String decode(String data) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(data);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Bidirectional mutations (Gmail first) ───────────────────────────────

    /**
     * Modify Gmail labels; returns the updated Message (with labelIds) from Gmail.
     * Throws on failure so callers do not update the local DB.
     */
    public Message modifyLabelsInGmail(Long userId, String gmailMessageId,
                                       List<String> addLabelIds, List<String> removeLabelIds) {
        if (gmailMessageId == null || gmailMessageId.isBlank()) {
            throw new NexoraException("Missing Gmail message id", 400);
        }
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NexoraException("User not found", 404));
            if (user.getGmailAccessToken() == null) {
                throw new NexoraException("No Gmail connection", 400);
            }
            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);

            ModifyMessageRequest modifyRequest = new ModifyMessageRequest();
            if (addLabelIds != null && !addLabelIds.isEmpty()) {
                modifyRequest.setAddLabelIds(addLabelIds);
            }
            if (removeLabelIds != null && !removeLabelIds.isEmpty()) {
                modifyRequest.setRemoveLabelIds(removeLabelIds);
            }

            Message updated = gmail.users().messages()
                    .modify("me", gmailMessageId, modifyRequest)
                    .execute();
            log.info("Modified Gmail message {} labels for user {} (add={}, remove={})",
                    gmailMessageId, userId, addLabelIds, removeLabelIds);
            return updated;
        } catch (NexoraException e) {
            throw e;
        } catch (GoogleJsonResponseException e) {
            log.warn("Gmail API error modifying message {} for user {}: {}",
                    gmailMessageId, userId, e.getDetails());
            throw new NexoraException("Gmail modify failed: " + e.getMessage(), e.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to modify Gmail message {}: {}", gmailMessageId, e.getMessage());
            throw new NexoraException("Gmail modify failed: " + e.getMessage(), 400);
        }
    }

    public Message markReadInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, null, List.of("UNREAD"));
    }

    public Message markUnreadInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("UNREAD"), null);
    }

    public Message starInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("STARRED"), null);
    }

    public Message unstarInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, null, List.of("STARRED"));
    }

    public Message archiveInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, null, List.of("INBOX"));
    }

    public Message moveToInboxInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("INBOX"), List.of("TRASH"));
    }

    public Message trashInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("TRASH"), List.of("INBOX"));
    }

    public Message restoreFromTrashInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("INBOX"), List.of("TRASH"));
    }

    /**
     * Apply Gmail-authoritative label state from a modify response onto a local email.
     */
    public void applyGmailMessageLabels(Email email, Message gmailMessage) {
        if (gmailMessage == null) return;
        List<String> labelIds = gmailMessage.getLabelIds() != null
                ? gmailMessage.getLabelIds() : List.of();
        applyLabelFields(email, labelIds);
        email.setGmailLabelIds(labelsToJson(labelIds));
    }

    private Long toLong(Integer value) {
        return value != null ? value.longValue() : null;
    }

    private void sleep(long ms) {
        try { java.lang.Thread.sleep(ms); } catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
    }

    /** Signals that stored historyId is invalid/expired and a full sync is required. */
    private static class HistoryOutOfDateException extends Exception {
        HistoryOutOfDateException(String message) {
            super(message);
        }
    }
}
