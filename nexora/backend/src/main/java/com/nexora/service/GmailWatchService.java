package com.nexora.service;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.WatchRequest;
import com.google.api.services.gmail.model.WatchResponse;
import com.nexora.config.GmailConfig;
import com.nexora.model.User;
import com.nexora.repository.UserRepository;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GmailWatchService {

    private final GmailConfig gmailConfig;
    private final UserRepository userRepository;
    private final TokenEncryptor tokenEncryptor;

    @Value("${google.pubsub-topic:}")
    private String pubsubTopic;

    /**
     * Register Gmail users.watch. No-op when {@code GOOGLE_PUBSUB_TOPIC} is blank.
     */
    @Transactional
    public void setupWatch(Long userId) {
        if (pubsubTopic == null || pubsubTopic.isBlank()) {
            log.info("Gmail watch skipped for user {} — GOOGLE_PUBSUB_TOPIC not configured", userId);
            return;
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getGmailAccessToken() == null) {
            return;
        }
        try {
            ensureFreshToken(user);
            Gmail gmail = buildGmail(user);
            WatchRequest watchRequest = new WatchRequest()
                    .setTopicName(pubsubTopic.trim())
                    .setLabelIds(List.of("INBOX"));
            WatchResponse watchResponse = gmail.users().watch("me", watchRequest).execute();
            if (watchResponse.getExpiration() != null) {
                user.setWatchExpiration(LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(watchResponse.getExpiration()),
                        ZoneId.systemDefault()));
            }
            if (watchResponse.getHistoryId() != null) {
                user.setWatchResourceId(watchResponse.getHistoryId().toString());
            }
            userRepository.save(user);
            log.info("Gmail watch registered for user {} until {}", userId, user.getWatchExpiration());
        } catch (Exception e) {
            log.warn("Gmail watch setup failed for user {}: {}", userId, e.getMessage());
        }
    }

    @Transactional
    public void renewWatchIfNeeded(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getGmailAccessToken() == null) {
            return;
        }
        LocalDateTime expiry = user.getWatchExpiration();
        if (expiry == null || expiry.isBefore(LocalDateTime.now().plusDays(1))) {
            setupWatch(userId);
        }
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
            return;
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
            if (responseBody != null && responseBody.containsKey("access_token")) {
                String newAccessToken = (String) responseBody.get("access_token");
                Number expiresIn = (Number) responseBody.get("expires_in");
                long seconds = expiresIn != null ? expiresIn.longValue() : 3600L;
                String encrypted = tokenEncryptor.encrypt(newAccessToken);
                LocalDateTime tokenExpiry = LocalDateTime.now().plusSeconds(seconds);
                user.setGmailAccessToken(encrypted);
                user.setTokenExpiry(tokenExpiry);
                userRepository.updateAccessToken(user.getId(), encrypted, tokenExpiry);
            }
        } catch (Exception e) {
            log.warn("Token refresh before watch failed for user {}: {}", user.getId(), e.getMessage());
        }
    }
}
