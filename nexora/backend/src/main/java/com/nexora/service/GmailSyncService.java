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
import com.nexora.model.User;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
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

    public GmailSyncResponse syncInbox(Long userId) {
        if (!activeSyncs.add(userId)) {
            log.info("Gmail sync already in progress for user {} — skipping concurrent request", userId);
            return emptyResponse("Sync already in progress");
        }

        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NexoraException("User not found", 404));

            if (user.getGmailAccessToken() == null) {
                log.warn("User {} has no Gmail access token — skipping sync", userId);
                return emptyResponse("No Gmail connection — connect your account first");
            }

            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);

            // Accurate label counts straight from Gmail (inbox, unread, spam, draft, purchases, etc.)
            Map<String, GmailLabelCountResponse> labelCounts = fetchAndCacheLabelCounts(gmail, user);

            // Fetch every message in INBOX — paginate until exhausted
            List<Message> inboxMessages = listMessagesByLabel(gmail, "INBOX", Integer.MAX_VALUE);
            log.info("Fetched {} INBOX message refs for user {}", inboxMessages.size(), userId);

            int newCount = 0;
            int updatedCount = 0;

            Set<String> seenInboxIds = new HashSet<>();
            int[] inboxStats = upsertMessageBatch(gmail, user, inboxMessages, MailboxKind.INBOX, seenInboxIds);
            newCount += inboxStats[0];
            updatedCount += inboxStats[1];

            // Messages that left INBOX in Gmail → archived locally
            updatedCount += markRemovedFromInbox(userId, seenInboxIds);

            // Gmail drafts (DRAFT label)
            List<Message> draftMessages = listMessagesByLabel(gmail, "DRAFT", Integer.MAX_VALUE);
            log.info("Fetched {} DRAFT message refs for user {}", draftMessages.size(), userId);
            int[] draftStats = upsertMessageBatch(gmail, user, draftMessages, MailboxKind.DRAFT, null);
            newCount += draftStats[0];
            updatedCount += draftStats[1];

            // Archived mail: not inbox / trash / spam / draft (cap to keep sync practical)
            List<Message> archivedMessages = listMessagesByQuery(
                    gmail, "-in:inbox -in:trash -in:spam -in:drafts", 300);
            log.info("Fetched {} archived message refs for user {}", archivedMessages.size(), userId);
            int[] archiveStats = upsertMessageBatch(gmail, user, archivedMessages, MailboxKind.ARCHIVE, null);
            newCount += archiveStats[0];
            updatedCount += archiveStats[1];

            try {
                Profile profile = gmail.users().getProfile("me").execute();
                if (profile.getHistoryId() != null) {
                    user.setGmailHistoryId(String.valueOf(profile.getHistoryId()));
                }
            } catch (Exception e) {
                log.warn("Could not fetch Gmail profile historyId for user {}: {}", userId, e.getMessage());
            }

            user.setLastSyncedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Sync complete for user {}: {} new, {} updated (inbox={}, drafts={}, archive={})",
                    userId, newCount, updatedCount,
                    inboxMessages.size(), draftMessages.size(), archivedMessages.size());

            return new GmailSyncResponse(
                    "Sync completed successfully",
                    newCount,
                    updatedCount,
                    inboxMessages.size(),
                    labelCounts
            );

        } catch (GeneralSecurityException | IOException e) {
            log.error("Gmail sync failed for user {}: {}", userId, e.getMessage());
            throw new NexoraException("Gmail sync failed: " + e.getMessage(), 400);
        } finally {
            activeSyncs.remove(userId);
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
                var existingOpt = emailRepository.findByGmailMessageId(stub.getId());
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

    private void applyFullMessageUpdate(Email email, Message full, MailboxKind kind) {
        List<String> labelIds = full.getLabelIds() != null ? full.getLabelIds() : List.of();
        applyLabelFields(email, labelIds);
        email.setGmailLabelIds(labelsToJson(labelIds));
        if (full.getSnippet() != null) {
            email.setBodySnippet(truncate(full.getSnippet(), 500));
        }
        applyMailboxKind(email, kind);
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

    private List<Message> listAllInboxMessages(Gmail gmail) throws IOException {
        return listMessagesByLabel(gmail, "INBOX", Integer.MAX_VALUE);
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
        boolean hasAttach = payload != null && hasAttachments(payload);

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

        String bodyText = payload != null ? extractBody(payload) : "";
        String snippet = message.getSnippet();

        Email email = Email.builder()
                .user(user)
                .gmailMessageId(message.getId())
                .gmailThreadId(message.getThreadId())
                .senderName(senderName)
                .senderEmail(senderEmail)
                .subject(subject)
                .bodySnippet(truncate(snippet, 500))
                .bodyFull(bodyText)
                .receivedAt(received)
                .hasAttachments(hasAttach)
                .recipientTo(to)
                .recipientCc(cc)
                .recipientBcc(bcc)
                .replyTo(replyTo.isBlank() ? null : replyTo)
                .sizeEstimate(message.getSizeEstimate() != null ? message.getSizeEstimate().longValue() : null)
                .gmailLabelIds(labelsToJson(labelIds))
                .category(Email.EmailCategory.UNCATEGORIZED)
                .priority(Email.Priority.MEDIUM)
                .build();

        applyLabelFields(email, labelIds);
        return email;
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

    private GmailSyncResponse emptyResponse(String message) {
        return new GmailSyncResponse(message, 0, 0, 0, Map.of());
    }

    // ─── Parsing helpers ─────────────────────────────────────────────────────

    private String getHeader(List<MessagePartHeader> headers, String name) {
        return headers.stream()
                .filter(h -> h != null && h.getName() != null && h.getName().equalsIgnoreCase(name))
                .map(MessagePartHeader::getValue)
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

    private String extractBody(MessagePart payload) {
        String text = extractPart(payload, "text/plain");
        if (text != null && !text.isBlank()) return text;

        String html = extractPart(payload, "text/html");
        if (html != null && !html.isBlank()) {
            return HTML_TAGS.matcher(html).replaceAll(" ").trim();
        }
        return "";
    }

    private String extractPart(MessagePart payload, String mimeType) {
        if (payload == null) return null;

        if (mimeType.equals(payload.getMimeType()) && payload.getBody() != null) {
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

    private boolean hasAttachments(MessagePart payload) {
        if (payload.getFilename() != null && !payload.getFilename().isEmpty()) {
            return true;
        }
        if (payload.getParts() == null) return false;
        return payload.getParts().stream().anyMatch(this::hasAttachments);
    }

    public void markReadInGmail(Long userId, String gmailMessageId) {
        if (gmailMessageId == null || gmailMessageId.isBlank()) return;

        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getGmailAccessToken() == null) return;

            ensureFreshToken(user);
            Gmail gmail = buildGmailClient(user);

            ModifyMessageRequest modifyRequest = new ModifyMessageRequest();
            modifyRequest.setRemoveLabelIds(List.of("UNREAD"));

            gmail.users().messages().modify("me", gmailMessageId, modifyRequest).execute();
            log.info("Marked Gmail message {} as read for user {}", gmailMessageId, userId);

        } catch (GoogleJsonResponseException e) {
            log.warn("Gmail API error marking message {} as read for user {}: {}", gmailMessageId, userId, e.getDetails());
        } catch (Exception e) {
            log.error("Failed to mark Gmail message {} as read: {}", gmailMessageId, e.getMessage());
        }
    }

    private Long toLong(Integer value) {
        return value != null ? value.longValue() : null;
    }

    private void sleep(long ms) {
        try { java.lang.Thread.sleep(ms); } catch (InterruptedException e) { java.lang.Thread.currentThread().interrupt(); }
    }
}
