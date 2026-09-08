package com.nexora.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.nexora.config.GmailConfig;
import com.nexora.dto.request.DraftRequest;
import com.nexora.dto.response.DraftResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.EmailDraft;
import com.nexora.model.User;
import com.nexora.repository.EmailDraftRepository;
import com.nexora.repository.UserRepository;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * User-scoped drafts. Wire DTOs only — never serialize nested {@link User}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailDraftService {

    private final EmailDraftRepository draftRepository;
    private final UserRepository userRepository;
    private final GmailConfig gmailConfig;
    private final TokenEncryptor tokenEncryptor;

    @Transactional(readOnly = true)
    public List<DraftResponse> getUserDrafts(Long userId) {
        requireUserId(userId);
        return draftRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(EmailDraftService::toResponse)
                .toList();
    }

    @Transactional
    public DraftResponse createDraft(Long userId, DraftRequest request) {
        requireUserId(userId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft body is required");
        }

        User user = userRepository.getReferenceById(userId);
        EmailDraft draft = new EmailDraft();
        draft.setUser(user);
        applyRequest(draft, request);
        if (draft.getDraftStatus() == null || draft.getDraftStatus().isBlank()) {
            draft.setDraftStatus(draft.getScheduledSendTime() != null ? "SCHEDULED" : "DRAFT");
        }
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public DraftResponse updateDraft(Long userId, Long id, DraftRequest request) {
        requireUserId(userId);
        requireId(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft body is required");
        }

        EmailDraft existing = owned(userId, id);
        applyRequest(existing, request);
        if (existing.getDraftStatus() == null || existing.getDraftStatus().isBlank()) {
            existing.setDraftStatus(existing.getScheduledSendTime() != null ? "SCHEDULED" : "DRAFT");
        }
        return toResponse(draftRepository.save(existing));
    }

    @Transactional
    public void deleteDraft(Long userId, Long id) {
        requireUserId(userId);
        requireId(id);
        draftRepository.delete(owned(userId, id));
    }

    @Transactional
    public String sendDraft(Long userId, Long id) {
        requireUserId(userId);
        requireId(id);
        EmailDraft draft = owned(userId, id);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized"));
        if (user.getGmailAccessToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Gmail is not connected");
        }
        if (draft.getTo() == null || draft.getTo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft needs a To address");
        }

        try {
            ensureFreshToken(user);
            Gmail gmail = buildGmail(user);
            Message message = new Message();
            message.setRaw(encodeMime(draft, user.getEmail()));
            gmail.users().messages().send("me", message).execute();
            draft.setDraftStatus("SENT");
            draftRepository.save(draft);
            return "Draft sent via Gmail";
        } catch (ResponseStatusException e) {
            throw e;
        } catch (NexoraException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode());
            throw new ResponseStatusException(
                    status != null ? status : HttpStatus.BAD_GATEWAY, e.getMessage());
        } catch (Exception e) {
            log.error("Draft send failed for user {} draft {}: {}", userId, id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Failed to send via Gmail. Ensure gmail.send (or gmail.modify) scope is granted: "
                            + e.getMessage());
        }
    }

    private static String encodeMime(EmailDraft draft, String from) {
        StringBuilder mime = new StringBuilder();
        mime.append("From: ").append(from != null ? from : "").append("\r\n");
        mime.append("To: ").append(draft.getTo().trim()).append("\r\n");
        if (draft.getCc() != null && !draft.getCc().isBlank()) {
            mime.append("Cc: ").append(draft.getCc().trim()).append("\r\n");
        }
        if (draft.getBcc() != null && !draft.getBcc().isBlank()) {
            mime.append("Bcc: ").append(draft.getBcc().trim()).append("\r\n");
        }
        String subject = draft.getSubject() != null ? draft.getSubject() : "";
        mime.append("Subject: ").append(encodeHeader(subject)).append("\r\n");
        mime.append("MIME-Version: 1.0\r\n");
        if (draft.getHtmlBody() != null && !draft.getHtmlBody().isBlank()) {
            mime.append("Content-Type: text/html; charset=\"UTF-8\"\r\n\r\n");
            mime.append(draft.getHtmlBody());
        } else {
            mime.append("Content-Type: text/plain; charset=\"UTF-8\"\r\n\r\n");
            mime.append(draft.getBody() != null ? draft.getBody() : "");
        }
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(mime.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String encodeHeader(String value) {
        // Keep ASCII subjects plain; encode non-ASCII per RFC 2047 lightly.
        boolean needsEncode = false;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 127) {
                needsEncode = true;
                break;
            }
        }
        if (!needsEncode) {
            return value.replace("\r", " ").replace("\n", " ");
        }
        String b64 = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return "=?UTF-8?B?" + b64 + "?=";
    }

    private Gmail buildGmail(User user) throws Exception {
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
        if (user.getTokenExpiry() != null && !user.getTokenExpiry().isBefore(now.plusMinutes(5))) {
            return;
        }
        if (user.getGmailRefreshToken() == null) {
            throw new NexoraException("No refresh token available", 401);
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
        try {
            @SuppressWarnings("rawtypes")
            org.springframework.http.ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://oauth2.googleapis.com/token",
                    new org.springframework.http.HttpEntity<>(body, headers),
                    Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = response.getBody();
            if (responseBody == null || !responseBody.containsKey("access_token")) {
                throw new NexoraException("Failed to refresh Gmail token", 401);
            }
            String newAccessToken = (String) responseBody.get("access_token");
            Number expiresIn = (Number) responseBody.get("expires_in");
            long seconds = expiresIn != null ? expiresIn.longValue() : 3600L;
            String encrypted = tokenEncryptor.encrypt(newAccessToken);
            LocalDateTime expiry = LocalDateTime.now().plusSeconds(seconds);
            user.setGmailAccessToken(encrypted);
            user.setTokenExpiry(expiry);
            userRepository.updateAccessToken(user.getId(), encrypted, expiry);
        } catch (NexoraException e) {
            throw e;
        } catch (Exception e) {
            throw new NexoraException("Failed to refresh Gmail access token: " + e.getMessage(), 401);
        }
    }

    private EmailDraft owned(Long userId, Long id) {
        return draftRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft " + id + " not found"));
    }

    private static void applyRequest(EmailDraft draft, DraftRequest request) {
        if (request.getTo() != null) draft.setTo(request.getTo());
        if (request.getCc() != null) draft.setCc(request.getCc());
        if (request.getBcc() != null) draft.setBcc(request.getBcc());
        if (request.getSubject() != null) draft.setSubject(request.getSubject());
        if (request.getBody() != null) draft.setBody(request.getBody());
        if (request.getHtmlBody() != null) draft.setHtmlBody(request.getHtmlBody());
        if (request.getScheduledSendTime() != null) draft.setScheduledSendTime(request.getScheduledSendTime());
        if (request.getDraftStatus() != null && !request.getDraftStatus().isBlank()) {
            draft.setDraftStatus(request.getDraftStatus().trim());
        }
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft id is required");
        }
    }

    private static DraftResponse toResponse(EmailDraft draft) {
        DraftResponse response = new DraftResponse();
        response.setId(draft.getId());
        response.setTo(draft.getTo());
        response.setCc(draft.getCc());
        response.setBcc(draft.getBcc());
        response.setSubject(draft.getSubject());
        response.setBody(draft.getBody());
        response.setHtmlBody(draft.getHtmlBody());
        response.setScheduledSendTime(draft.getScheduledSendTime());
        response.setDraftStatus(draft.getDraftStatus());
        response.setCreatedAt(draft.getCreatedAt());
        response.setUpdatedAt(draft.getUpdatedAt());
        return response;
    }
}
