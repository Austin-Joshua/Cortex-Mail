package com.nexora.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.googleapis.json.GoogleJsonErrorContainer;
import com.google.api.client.http.HttpHeaders;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@Service
@Slf4j
public class GmailSyncService {

    private final GmailConfig gmailConfig;
    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final TokenEncryptor tokenEncryptor;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate persistTransaction;
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> activeSyncs =
            new java.util.concurrent.ConcurrentHashMap<>();

    private static final long SYNC_LOCK_TTL_MS = 20 * 60 * 1000L;

    public GmailSyncService(GmailConfig gmailConfig,
                            UserRepository userRepository,
                            EmailRepository emailRepository,
                            TokenEncryptor tokenEncryptor,
                            ObjectMapper objectMapper,
                            PlatformTransactionManager transactionManager) {
        this.gmailConfig = gmailConfig;
        this.userRepository = userRepository;
        this.emailRepository = emailRepository;
        this.tokenEncryptor = tokenEncryptor;
        this.objectMapper = objectMapper;
        this.persistTransaction = new TransactionTemplate(transactionManager);
    }

    private static final int PAGE_SIZE = 100;
    /** First sign-in: newest inbox only — rest loads in background. */
    private static final int MAX_INBOX_FIRST_SYNC = 100;
    /** Subsequent full sync inbox cap. */
    private static final int MAX_INBOX_FULL_SYNC = 500;
    private static final int MAX_DRAFT_FULL_SYNC = 200;
    private static final int MAX_ARCHIVE_FULL_SYNC = 300;
    private static final int GMAIL_BATCH_SIZE = 50;
    private static final int MAX_RETRIES = 3;
    private static final Pattern HTML_TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern CHARSET_PATTERN = Pattern.compile("charset\\s*=\\s*\"?([^\\s;\"]+)\"?", Pattern.CASE_INSENSITIVE);

    /**
     * Unified sync entry: prefers incremental when a historyId is stored;
     * falls back to full sync on first sync or invalid/expired history.
     * Not @Transactional — Gmail calls must not hold a JDBC connection open.
     */
    public GmailSyncResponse syncInbox(Long userId) {
        if (!tryAcquireSyncLock(userId)) {
            log.info("Gmail sync already in progress for user {} — skipping concurrent request", userId);
            return skippedResponse(userId, "Sync already in progress — try again in a moment");
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NexoraException("User not found", 404));

            if (user.getGmailAccessToken() == null) {
                log.warn("User {} has no Gmail access token — skipping sync", userId);
                return emptyResponse("No Gmail connection — connect your account first", null);
            }

            ensureFreshToken(user);
            try {
                return runSync(user);
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 401) {
                    log.warn("Gmail returned 401 for user {} — refreshing token and retrying once", userId);
                    refreshUserAccessToken(user);
                    return runSync(user);
                }
                throw e;
            }

        } catch (NexoraException e) {
            throw e;
        } catch (GoogleJsonResponseException e) {
            if (e.getStatusCode() == 401) {
                throw new NexoraException("Gmail authorization expired — reconnect your account", 401);
            }
            log.error("Gmail sync failed for user {}: {}", userId, e.getMessage());
            throw new NexoraException("Gmail sync failed: " + e.getMessage(), 502);
        } catch (GeneralSecurityException | IOException e) {
            log.error("Gmail sync failed for user {}: {}", userId, e.getMessage());
            throw new NexoraException("Gmail sync failed: " + e.getMessage(), 502);
        } finally {
            releaseSyncLock(userId);
        }
    }

    /** True while this JVM holds an active sync lock for the user (TTL-aware). */
    public boolean hasActiveSync(Long userId) {
        if (userId == null) {
            return false;
        }
        Long started = activeSyncs.get(userId);
        if (started == null) {
            return false;
        }
        if (System.currentTimeMillis() - started >= SYNC_LOCK_TTL_MS) {
            activeSyncs.remove(userId, started);
            return false;
        }
        return true;
    }

    private GmailSyncResponse runSync(User user) throws IOException, GeneralSecurityException {
        Gmail gmail = buildGmailClient(user);
        if (user.getGmailHistoryId() != null && !user.getGmailHistoryId().isBlank()) {
            try {
                return syncIncremental(gmail, user);
            } catch (HistoryOutOfDateException e) {
                log.warn("History ID invalid for user {} — falling back to full sync: {}",
                        user.getId(), e.getMessage());
                return fullSync(gmail, user);
            }
        }
        return fullSync(gmail, user);
    }

    private GmailSyncResponse fullSync(Gmail gmail, User user) throws IOException {
        Long userId = user.getId();
        boolean fastFirstLoad = user.getLastSyncedAt() == null;
        int inboxCap = fastFirstLoad ? MAX_INBOX_FIRST_SYNC : MAX_INBOX_FULL_SYNC;

        Map<String, GmailLabelCountResponse> labelCounts = fetchAndCacheLabelCounts(gmail, user);

        List<Message> inboxMessages = listMessagesByLabel(gmail, "INBOX", inboxCap);
        log.info("Fetched {} INBOX message refs for user {} (cap {}, fastFirst={})",
                inboxMessages.size(), userId, inboxCap, fastFirstLoad);

        int newCount = 0;
        int updatedCount = 0;

        Set<String> seenInboxIds = new HashSet<>();
        int[] inboxStats = upsertMessageBatch(gmail, user, inboxMessages, MailboxKind.INBOX, seenInboxIds, fastFirstLoad);
        newCount += inboxStats[0];
        updatedCount += inboxStats[1];

        if (!fastFirstLoad) {
            if (inboxMessages.size() < inboxCap) {
                updatedCount += markRemovedFromInbox(userId, seenInboxIds);
            } else {
                log.info("Skipping inbox prune for user {} — listing hit cap {}", userId, inboxCap);
            }

            List<Message> draftMessages = listMessagesByLabel(gmail, "DRAFT", MAX_DRAFT_FULL_SYNC);
            log.info("Fetched {} DRAFT message refs for user {}", draftMessages.size(), userId);
            int[] draftStats = upsertMessageBatch(gmail, user, draftMessages, MailboxKind.DRAFT, null, false);
            newCount += draftStats[0];
            updatedCount += draftStats[1];

            List<Message> archivedMessages = listMessagesByQuery(
                    gmail, "-in:inbox -in:trash -in:spam -in:drafts", MAX_ARCHIVE_FULL_SYNC);
            log.info("Fetched {} archived message refs for user {}", archivedMessages.size(), userId);
            int[] archiveStats = upsertMessageBatch(gmail, user, archivedMessages, MailboxKind.ARCHIVE, null, false);
            newCount += archiveStats[0];
            updatedCount += archiveStats[1];
        }

        // FAST_FIRST must not persist historyId. Incremental after a failed
        // background pass would skip remaining inbox, drafts, and archive.
        if (!fastFirstLoad) {
            storeProfileHistoryId(gmail, user);
        }
        user.setLastSyncedAt(LocalDateTime.now());
        userRepository.save(user);

        String syncMode = fastFirstLoad ? "FAST_FIRST" : "FULL";
        String message = fastFirstLoad
                ? "Fast inbox sync complete — remaining mail loads in background"
                : "Full sync completed successfully";

        log.info("{} sync complete for user {}: {} new, {} updated (inbox={})",
                syncMode, userId, newCount, updatedCount, inboxMessages.size());

        return new GmailSyncResponse(
                message,
                newCount,
                updatedCount,
                inboxMessages.size(),
                labelCounts,
                syncMode
        );
    }

    /**
     * Background pass after FAST_FIRST: full inbox remainder, drafts, and archive.
     */
    public void syncSecondaryMailboxes(Long userId) {
        if (!tryAcquireSyncLock(userId)) {
            log.info("Secondary mailbox sync skipped for user {} — another sync is running", userId);
            return;
        }
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getGmailAccessToken() == null) {
                return;
            }
            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);

            List<Message> inboxMessages = listMessagesByLabel(gmail, "INBOX", MAX_INBOX_FULL_SYNC);
            Set<String> seenInboxIds = new HashSet<>();
            int[] inboxStats = upsertMessageBatch(gmail, user, inboxMessages, MailboxKind.INBOX, seenInboxIds, false);
            if (inboxMessages.size() < MAX_INBOX_FULL_SYNC) {
                markRemovedFromInbox(userId, seenInboxIds);
            } else {
                log.info("Skipping inbox prune on secondary sync for user {} — listing hit cap", userId);
            }

            touchSyncLock(userId);
            List<Message> draftMessages = listMessagesByLabel(gmail, "DRAFT", MAX_DRAFT_FULL_SYNC);
            int[] draftStats = upsertMessageBatch(gmail, user, draftMessages, MailboxKind.DRAFT, null, false);

            touchSyncLock(userId);
            List<Message> archivedMessages = listMessagesByQuery(
                    gmail, "-in:inbox -in:trash -in:spam -in:drafts", MAX_ARCHIVE_FULL_SYNC);
            int[] archiveStats = upsertMessageBatch(gmail, user, archivedMessages, MailboxKind.ARCHIVE, null, false);

            storeProfileHistoryId(gmail, user);
            user.setLastSyncedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Secondary mailbox sync for user {}: inbox +{} ~{}, drafts +{} ~{}, archive +{} ~{}",
                    userId, inboxStats[0], inboxStats[1], draftStats[0], draftStats[1], archiveStats[0], archiveStats[1]);
        } catch (Exception e) {
            log.error("Secondary mailbox sync failed for user {}: {}", userId, e.getMessage());
            // Still capture historyId when possible so the UI does not stay on "enriching" forever.
            try {
                User user = userRepository.findById(userId).orElse(null);
                if (user != null && user.getGmailAccessToken() != null) {
                    ensureFreshToken(user);
                    Gmail gmail = buildGmailClient(user);
                    storeProfileHistoryId(gmail, user);
                    if (user.getLastSyncedAt() == null) {
                        user.setLastSyncedAt(LocalDateTime.now());
                    }
                    userRepository.save(user);
                }
            } catch (Exception nested) {
                log.warn("Could not store historyId after secondary failure for user {}: {}",
                        userId, nested.getMessage());
            }
        } finally {
            releaseSyncLock(userId);
        }
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
                Gmail.Users.History.List request = gmail.users().history().list("me")
                        .setStartHistoryId(startHistoryId)
                        .setMaxResults((long) PAGE_SIZE);
                if (pageToken != null) {
                    request.setPageToken(pageToken);
                }
                response = request.execute();
            } catch (GoogleJsonResponseException e) {
                if (e.getStatusCode() == 404 || isInvalidHistoryId(e)) {
                    throw new HistoryOutOfDateException("history.list rejected startHistoryId ("
                            + e.getStatusCode() + ")");
                }
                throw e;
            }

            // Gmail's next cursor is response.historyId — never a per-record id.
            if (response.getHistoryId() != null) {
                latestHistoryId = response.getHistoryId();
            }

            List<History> histories = response.getHistory();
            if (histories != null && !histories.isEmpty()) {
                int[] counts = processHistoryRecords(gmail, user, histories);
                newCount += counts[0];
                updatedCount += counts[1];
                deletedCount += counts[2];
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
    private int[] processHistoryRecords(Gmail gmail, User user, List<History> histories) {
        Long userId = user.getId();
        Set<String> addIds = new LinkedHashSet<>();
        Set<String> deleteIds = new LinkedHashSet<>();
        Set<String> labelIds = new LinkedHashSet<>();

        for (History history : histories) {
            if (history.getMessagesAdded() != null) {
                for (HistoryMessageAdded added : history.getMessagesAdded()) {
                    if (added.getMessage() != null && added.getMessage().getId() != null) {
                        addIds.add(added.getMessage().getId());
                    }
                }
            }
            if (history.getMessagesDeleted() != null) {
                for (HistoryMessageDeleted deleted : history.getMessagesDeleted()) {
                    if (deleted.getMessage() != null && deleted.getMessage().getId() != null) {
                        deleteIds.add(deleted.getMessage().getId());
                    }
                }
            }
            if (history.getLabelsAdded() != null) {
                for (HistoryLabelAdded labelsAdded : history.getLabelsAdded()) {
                    if (labelsAdded.getMessage() != null && labelsAdded.getMessage().getId() != null) {
                        labelIds.add(labelsAdded.getMessage().getId());
                    }
                }
            }
            if (history.getLabelsRemoved() != null) {
                for (HistoryLabelRemoved labelsRemoved : history.getLabelsRemoved()) {
                    if (labelsRemoved.getMessage() != null && labelsRemoved.getMessage().getId() != null) {
                        labelIds.add(labelsRemoved.getMessage().getId());
                    }
                }
            }
        }

        // Adds already upsert full state; deletes remove the row — skip redundant work.
        labelIds.removeAll(addIds);
        deleteIds.removeAll(addIds);
        labelIds.removeAll(deleteIds);

        int newCount = 0;
        int updatedCount = 0;
        int deletedCount = 0;

        List<String> toFetch = new ArrayList<>(addIds.size() + labelIds.size());
        toFetch.addAll(addIds);
        toFetch.addAll(labelIds);

        Map<String, Message> fetched = Map.of();
        if (!toFetch.isEmpty()) {
            try {
                fetched = fetchMessagesInBatches(gmail, toFetch, false);
            } catch (IOException e) {
                log.warn("Batch history fetch failed — falling back to sequential: {}", e.getMessage());
            }
        }

        Map<String, Email> existingByMessageId = new HashMap<>();
        if (!toFetch.isEmpty()) {
            for (Email existing : emailRepository.findByUserIdAndGmailMessageIdIn(userId, toFetch)) {
                existingByMessageId.put(existing.getGmailMessageId(), existing);
            }
        }

        List<Email> toSave = new ArrayList<>();
        for (String messageId : addIds) {
            try {
                Message full = fetched.get(messageId);
                if (full == null) {
                    full = fetchWithRetry(gmail, messageId);
                }
                if (full == null) continue;

                Email existing = existingByMessageId.get(messageId);
                if (existing != null) {
                    applyFullMessageUpdate(existing, full, null);
                    toSave.add(existing);
                    updatedCount++;
                } else {
                    Email email = parseMessage(full, user);
                    toSave.add(email);
                    existingByMessageId.put(messageId, email);
                    newCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process messagesAdded {}: {}", messageId, e.getMessage());
            }
        }

        for (String messageId : labelIds) {
            try {
                Message full = fetched.get(messageId);
                if (full == null) {
                    full = fetchWithRetry(gmail, messageId);
                }
                if (full == null) {
                    log.warn("Could not fetch message {} for label history; skipping local update", messageId);
                    continue;
                }

                Email existing = existingByMessageId.get(messageId);
                if (existing == null) {
                    Email email = parseMessage(full, user);
                    toSave.add(email);
                    existingByMessageId.put(messageId, email);
                    newCount++;
                } else {
                    List<String> labelIdList = full.getLabelIds() != null ? full.getLabelIds() : List.of();
                    applyLabelFields(existing, labelIdList);
                    existing.setGmailLabelIds(labelsToJson(labelIdList));
                    toSave.add(existing);
                    updatedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to process label history {}: {}", messageId, e.getMessage());
            }
        }

        if (!toSave.isEmpty()) {
            try {
                emailRepository.saveAll(toSave);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Bulk history save hit a concurrent duplicate; retrying individually");
                for (Email email : toSave) {
                    try {
                        emailRepository.save(email);
                    } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
                        log.debug("Message {} was already saved concurrently", email.getGmailMessageId());
                    }
                }
            }
        }

        if (!deleteIds.isEmpty()) {
            deletedCount = Objects.requireNonNullElse(
                    persistTransaction.execute(status ->
                            emailRepository.deleteByUserIdAndGmailMessageIdIn(userId, deleteIds)),
                    0);
        }

        return new int[]{newCount, updatedCount, deletedCount};
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

    public long getInboxUnreadCount(Long userId) {
        Map<String, GmailLabelCountResponse> counts = getLabelCounts(userId);
        GmailLabelCountResponse inbox = counts.get("INBOX");
        if (inbox != null && inbox.getMessagesUnread() != null) {
            return inbox.getMessagesUnread();
        }
        return emailRepository.countInboxUnreadByUserId(userId);
    }

    // ─── Gmail API helpers ───────────────────────────────────────────────────

    private static final List<String> LABEL_COUNT_IDS = List.of(
            "INBOX", "DRAFT", "IMPORTANT", "SPAM", "TRASH", "STARRED", "UNREAD",
            "CATEGORY_PERSONAL", "CATEGORY_PROMOTIONS", "CATEGORY_UPDATES",
            "CATEGORY_FORUMS", "CATEGORY_SOCIAL"
    );

    /**
     * labels.list does not return message/thread counts — those require labels.get.
     * Batch-get the system labels we surface in the UI / integrity checks.
     */
    private Map<String, GmailLabelCountResponse> fetchAndCacheLabelCounts(Gmail gmail, User user)
            throws IOException {
        Map<String, GmailLabelCountResponse> counts = new LinkedHashMap<>();
        BatchRequest batch = gmail.batch();

        for (String labelId : LABEL_COUNT_IDS) {
            batch.queue(
                    gmail.users().labels().get("me", labelId).buildHttpRequest(),
                    Label.class,
                    GoogleJsonErrorContainer.class,
                    new JsonBatchCallback<Label>() {
                        @Override
                        public void onSuccess(Label label, HttpHeaders responseHeaders) {
                            if (label == null || label.getId() == null) return;
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

                        @Override
                        public void onFailure(GoogleJsonError error, HttpHeaders responseHeaders) {
                            log.debug("Label get skipped for {}: {}", labelId,
                                    error != null ? error.getMessage() : "unknown");
                        }
                    });
        }
        try {
            batch.execute();
        } catch (IOException e) {
            log.warn("Label count batch failed for user {}: {}", user.getId(), e.getMessage());
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
                                     MailboxKind kind, Set<String> collectIds,
                                     boolean metadataOnly) {
        int newCount = 0;
        int updatedCount = 0;
        Set<String> seen = new HashSet<>();
        List<String> idsToFetch = new ArrayList<>();

        for (Message stub : stubs) {
            if (stub.getId() == null || !seen.add(stub.getId())) continue;
            if (collectIds != null) collectIds.add(stub.getId());
            idsToFetch.add(stub.getId());
        }

        Map<String, Message> fetched = Map.of();
        if (!idsToFetch.isEmpty()) {
            try {
                fetched = fetchMessagesInBatches(gmail, idsToFetch, metadataOnly);
            } catch (IOException e) {
                log.warn("Batch Gmail fetch failed — falling back to sequential: {}", e.getMessage());
            }
        }

        Map<String, Email> existingByMessageId = new HashMap<>();
        if (!idsToFetch.isEmpty()) {
            for (Email existing : emailRepository.findByUserIdAndGmailMessageIdIn(user.getId(), idsToFetch)) {
                existingByMessageId.put(existing.getGmailMessageId(), existing);
            }
        }

        List<Email> toSave = new ArrayList<>(idsToFetch.size());
        for (String messageId : idsToFetch) {
            try {
                Message full = fetched.get(messageId);
                if (full == null) {
                    full = fetchWithRetry(gmail, messageId);
                }
                if (full == null) continue;

                Email existing = existingByMessageId.get(messageId);
                if (existing != null) {
                    applyFullMessageUpdate(existing, full, kind);
                    toSave.add(existing);
                    updatedCount++;
                    continue;
                }

                Email email = parseMessage(full, user);
                applyMailboxKindIfNoLabels(email, full, kind);
                toSave.add(email);
                newCount++;
            } catch (Exception e) {
                log.error("Failed to sync message {}: {}", messageId, e.getMessage());
            }
        }

        if (!toSave.isEmpty()) {
            try {
                emailRepository.saveAll(toSave);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.warn("Bulk message save encountered a concurrent duplicate; retrying individually");
                newCount = 0;
                updatedCount = 0;
                for (Email email : toSave) {
                    try {
                        boolean existed = existingByMessageId.containsKey(email.getGmailMessageId());
                        emailRepository.save(email);
                        if (existed) updatedCount++; else newCount++;
                    } catch (org.springframework.dao.DataIntegrityViolationException duplicate) {
                        log.debug("Message {} was already saved concurrently", email.getGmailMessageId());
                    }
                }
            }
        }
        return new int[]{newCount, updatedCount};
    }

    private Map<String, Message> fetchMessagesInBatches(Gmail gmail, List<String> messageIds, boolean metadataOnly)
            throws IOException {
        Map<String, Message> out = new HashMap<>();
        if (messageIds == null || messageIds.isEmpty()) {
            return out;
        }

        for (int offset = 0; offset < messageIds.size(); offset += GMAIL_BATCH_SIZE) {
            List<String> chunk = messageIds.subList(offset, Math.min(offset + GMAIL_BATCH_SIZE, messageIds.size()));
            int attempt = 0;
            while (true) {
                BatchRequest batch = gmail.batch();
                for (String id : chunk) {
                    Gmail.Users.Messages.Get get = gmail.users().messages().get("me", id);
                    if (metadataOnly) {
                        get.setFormat("METADATA");
                        get.setMetadataHeaders(Arrays.asList(
                                "Subject", "From", "To", "Cc", "Bcc", "Reply-To", "Date", "Message-ID"));
                    } else {
                        get.setFormat("FULL");
                    }
                    batch.queue(get.buildHttpRequest(), Message.class, GoogleJsonErrorContainer.class,
                            new JsonBatchCallback<Message>() {
                        @Override
                        public void onSuccess(Message message, HttpHeaders responseHeaders) {
                            if (message != null && message.getId() != null) {
                                out.put(message.getId(), message);
                            }
                        }

                        @Override
                        public void onFailure(GoogleJsonError error, HttpHeaders responseHeaders) {
                            log.warn("Batch fetch failed for message {}: {}", id,
                                    error != null ? error.getMessage() : "unknown");
                        }
                            });
                }
                try {
                    batch.execute();
                    break;
                } catch (GoogleJsonResponseException e) {
                    if (isRateLimited(e) && attempt < MAX_RETRIES) {
                        attempt++;
                        long waitMs = (long) Math.pow(2, attempt) * 1000;
                        log.warn("Gmail message batch rate-limited — waiting {}ms before retry {}", waitMs, attempt);
                        sleep(waitMs);
                        continue;
                    }
                    throw e;
                }
            }
        }
        return out;
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
            String from = getHeader(headers, "From");
            if (!from.isBlank()) {
                String[] fromParts = parseFrom(from);
                if (fromParts[0] != null && !fromParts[0].isBlank()) {
                    email.setSenderName(fromParts[0]);
                }
                if (fromParts[1] != null && !fromParts[1].isBlank()) {
                    email.setSenderEmail(fromParts[1].trim());
                }
            }
            String to = getHeader(headers, "To");
            if (!to.isBlank()) email.setRecipientTo(to);
            String cc = getHeader(headers, "Cc");
            if (!cc.isBlank()) email.setRecipientCc(cc);
            String bcc = getHeader(headers, "Bcc");
            if (!bcc.isBlank()) email.setRecipientBcc(bcc);
            String replyTo = getHeader(headers, "Reply-To");
            if (!replyTo.isBlank()) email.setReplyTo(replyTo);

            BodyContent body = extractBodyContent(payload);
            boolean hasStoredBody = (email.getBodyFull() != null && !email.getBodyFull().isBlank())
                    || (email.getBodyHtml() != null && !email.getBodyHtml().isBlank());
            boolean incomingEmpty = (body.plain == null || body.plain.isBlank())
                    && (body.html == null || body.html.isBlank());
            // METADATA fetches have no payload body — never wipe a previously stored FULL body.
            if (!(hasStoredBody && incomingEmpty)) {
                if (body.html != null && !body.html.isBlank()) {
                    email.setBodyHtml(body.html);
                }
                String plain = body.plain;
                if (plain == null || plain.isBlank()) {
                    plain = full.getSnippet() != null ? full.getSnippet() : email.getBodyFull();
                }
                if (plain != null && !plain.isBlank()) {
                    email.setBodyFull(plain);
                }
            }

            List<EmailAttachment> attachments = collectAttachments(payload);
            if (!attachments.isEmpty() || !hasStoredBody) {
                replaceAttachments(email, attachments);
                email.setHasAttachments(!attachments.isEmpty());
            }
        }

        applyMailboxKindIfNoLabels(email, full, kind);
    }

    /** Gmail labelIds are authoritative. MailboxKind is only a fallback when labels are missing. */
    private void applyMailboxKindIfNoLabels(Email email, Message full, MailboxKind kind) {
        if (kind == null) return;
        List<String> labelIds = full.getLabelIds();
        if (labelIds == null || labelIds.isEmpty()) {
            applyMailboxKind(email, kind);
        }
    }

    private void applyMailboxKind(Email email, MailboxKind kind) {
        switch (kind) {
            case INBOX:
                email.setInInbox(true);
                email.setIsDraft(false);
                email.setIsArchived(false);
                email.setIsTrash(false);
                email.setIsSpam(false);
                break;
            case DRAFT:
                email.setInInbox(false);
                email.setIsDraft(true);
                email.setIsArchived(false);
                email.setIsTrash(false);
                email.setIsSpam(false);
                break;
            case ARCHIVE:
                email.setInInbox(false);
                email.setIsDraft(false);
                email.setIsArchived(true);
                email.setIsTrash(false);
                email.setIsSpam(false);
                break;
            default:
                break;
        }
    }

    private List<Message> listMessagesByLabel(Gmail gmail, String labelId, int maxTotal) throws IOException {
        List<Message> all = new ArrayList<>();
        String nextPageToken = null;

        do {
            long pageSize = Math.min(PAGE_SIZE, maxTotal - all.size());
            if (pageSize <= 0) break;

            Gmail.Users.Messages.List request = gmail.users().messages()
                    .list("me")
                    .setLabelIds(Collections.singletonList(labelId))
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

            Gmail.Users.Messages.List request = gmail.users().messages()
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

        if (all.size() > maxTotal) {
            return all.subList(0, maxTotal);
        }
        return all;
    }

    private int markRemovedFromInbox(Long userId, Set<String> currentInboxIds) {
        if (currentInboxIds == null || currentInboxIds.isEmpty()) {
            return 0;
        }
        // Bulk UPDATE — never SELECT body TEXT / attachments into memory for prune.
        Integer changed = persistTransaction.execute(status -> {
            int archived = emailRepository.archiveInboxMissingFromGmail(userId, currentInboxIds);
            archived += emailRepository.archiveLegacyNullInboxMissingFromGmail(userId, currentInboxIds);
            archived += emailRepository.restoreLegacyNullInboxPresentInGmail(userId, currentInboxIds);
            return archived;
        });
        return changed != null ? changed : 0;
    }

    private boolean applyLabelFields(Email email, List<String> labelIds) {
        if (labelIds == null) {
            labelIds = List.of();
        }

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
        } catch (NexoraException e) {
            throw e;
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
                if (e.getStatusCode() == 404) {
                    log.debug("Gmail message {} no longer exists", messageId);
                    return null;
                }
                if (isRateLimited(e)) {
                    attempt++;
                    long waitMs = (long) Math.pow(2, attempt) * 1000;
                    log.warn("Rate limited — waiting {}ms before retry {} for {}", waitMs, attempt, messageId);
                    sleep(waitMs);
                    continue;
                }
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
                }
                log.error("Gmail API error for message {}: {}", messageId, e.getMessage());
                return null;
            } catch (IOException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) {
                    log.error("IO error fetching message {}: {}", messageId, e.getMessage());
                    return null;
                }
                sleep((long) Math.pow(2, attempt) * 500);
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
        String bodyFull = body.plain;
        if (bodyFull == null || bodyFull.isBlank()) {
            bodyFull = snippet != null ? snippet : "";
        }

        Email email = Email.builder()
                .user(user)
                .gmailMessageId(message.getId())
                .gmailThreadId(message.getThreadId())
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodySnippet(truncate(snippet, 500))
                .bodyFull(bodyFull)
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

    /**
     * Replace attachments in-place. Never call setAttachments(new …) on a managed
     * Email — Hibernate orphanRemoval rejects replacing the PersistentBag.
     * clear() + add within the sync transaction deletes orphans and adds new rows.
     */
    private void replaceAttachments(Email email, List<EmailAttachment> attachments) {
        List<EmailAttachment> bag = email.getAttachments();
        if (bag == null) {
            bag = new ArrayList<>();
            email.setAttachments(bag);
        } else {
            bag.clear();
        }
        for (EmailAttachment att : attachments) {
            att.setEmail(email);
            bag.add(att);
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

    /** Concurrent sync: return cached label counts so the UI stays usable. */
    private GmailSyncResponse skippedResponse(Long userId, String message) {
        Map<String, GmailLabelCountResponse> labels = getLabelCounts(userId);
        return new GmailSyncResponse(message, 0, 0, 0, labels, "SKIPPED");
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
        if (from == null || from.isBlank()) {
            return new String[]{"", ""};
        }
        int lt = from.indexOf('<');
        int gt = from.indexOf('>', lt + 1);
        if (lt >= 0 && gt > lt) {
            String name = from.substring(0, lt).trim().replace("\"", "");
            String email = from.substring(lt + 1, gt).trim();
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

        String partMime = payload.getMimeType();
        if (partMime != null && partMime.toLowerCase(Locale.ROOT).startsWith(mimeType.toLowerCase(Locale.ROOT))
                && payload.getBody() != null) {
            String data = payload.getBody().getData();
            if (data != null) return decode(data, charsetOf(payload));
        }

        if (payload.getParts() != null) {
            for (MessagePart part : payload.getParts()) {
                String result = extractPart(part, mimeType);
                if (result != null) return result;
            }
        }
        return null;
    }

    private Charset charsetOf(MessagePart part) {
        String contentType = "";
        if (part.getHeaders() != null) {
            contentType = getHeader(part.getHeaders(), "Content-Type");
        }
        if (contentType.isBlank() && part.getMimeType() != null) {
            contentType = part.getMimeType();
        }
        java.util.regex.Matcher matcher = CHARSET_PATTERN.matcher(contentType);
        if (matcher.find()) {
            try {
                return Charset.forName(matcher.group(1).trim());
            } catch (Exception ignored) {
                // fall through to UTF-8
            }
        }
        return StandardCharsets.UTF_8;
    }

    private String decode(String data, Charset charset) {
        try {
            String padded = data;
            int remainder = padded.length() % 4;
            if (remainder > 0) {
                padded = padded + "====".substring(remainder);
            }
            byte[] decoded = Base64.getUrlDecoder().decode(padded);
            return new String(decoded, charset != null ? charset : StandardCharsets.UTF_8);
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

    public Message markImportantInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("IMPORTANT"), null);
    }

    public Message unmarkImportantInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, null, List.of("IMPORTANT"));
    }

    public Message archiveInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, null, List.of("INBOX"));
    }

    public Message moveToInboxInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("INBOX"), List.of("TRASH", "SPAM"));
    }

    public Message trashInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("TRASH"), List.of("INBOX", "SPAM"));
    }

    public Message restoreFromTrashInGmail(Long userId, String gmailMessageId) {
        return modifyLabelsInGmail(userId, gmailMessageId, List.of("INBOX"), List.of("TRASH", "SPAM"));
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

    private boolean tryAcquireSyncLock(Long userId) {
        long now = System.currentTimeMillis();
        Long existing = activeSyncs.putIfAbsent(userId, now);
        if (existing == null) {
            return true;
        }
        if (now - existing >= SYNC_LOCK_TTL_MS) {
            log.warn("Stale Gmail sync lock for user {} — allowing new sync", userId);
            return activeSyncs.replace(userId, existing, now);
        }
        return false;
    }

    private void releaseSyncLock(Long userId) {
        activeSyncs.remove(userId);
    }

    private void touchSyncLock(Long userId) {
        activeSyncs.replace(userId, System.currentTimeMillis());
    }

    private boolean isRateLimited(GoogleJsonResponseException e) {
        if (e.getStatusCode() == 429) {
            return true;
        }
        if (e.getStatusCode() != 403) {
            return false;
        }
        String details = e.getDetails() != null ? String.valueOf(e.getDetails()) : "";
        String message = e.getMessage() != null ? e.getMessage() : "";
        String combined = (details + " " + message).toLowerCase(Locale.ROOT);
        return combined.contains("ratelimitexceeded")
                || combined.contains("userratelimitexceeded")
                || combined.contains("quotaexceeded");
    }

    private boolean isInvalidHistoryId(GoogleJsonResponseException e) {
        if (e.getStatusCode() != 400) {
            return false;
        }
        String details = e.getDetails() != null ? String.valueOf(e.getDetails()) : "";
        String message = e.getMessage() != null ? e.getMessage() : "";
        String combined = (details + " " + message).toLowerCase(Locale.ROOT);
        return combined.contains("historyid") || combined.contains("start history");
    }

    private void sleep(long ms) {
        try {
            java.lang.Thread.sleep(ms);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
        }
    }
}
