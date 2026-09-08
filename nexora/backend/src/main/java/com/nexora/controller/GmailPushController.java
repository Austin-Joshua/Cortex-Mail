package com.nexora.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.model.BackgroundJob;
import com.nexora.model.User;
import com.nexora.repository.UserRepository;
import com.nexora.service.BackgroundJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/gmail")
@RequiredArgsConstructor
@Slf4j
public class GmailPushController {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final BackgroundJobService backgroundJobService;

    /**
     * Pub/Sub push endpoint. Loosely verified — logs payload, parses historyId, enqueues incremental sync.
     */
    @PostMapping("/push")
    public ResponseEntity<Map<String, String>> push(@RequestBody(required = false) String body) {
        try {
            if (body == null || body.isBlank()) {
                log.info("Gmail push received empty body");
                return ResponseEntity.ok(Map.of("status", "ignored"));
            }
            JsonNode root = objectMapper.readTree(body);
            String historyId = null;
            String emailAddress = null;

            JsonNode message = root.path("message");
            if (!message.isMissingNode()) {
                String dataB64 = message.path("data").asText(null);
                if (dataB64 != null && !dataB64.isBlank()) {
                    String decoded = new String(Base64.getDecoder().decode(dataB64), StandardCharsets.UTF_8);
                    JsonNode notification = objectMapper.readTree(decoded);
                    historyId = textOrNull(notification, "historyId");
                    emailAddress = textOrNull(notification, "emailAddress");
                    log.info("Gmail push decoded historyId={} email={}", historyId, emailAddress);
                }
            }
            if (historyId == null) {
                historyId = textOrNull(root, "historyId");
            }
            if (emailAddress == null) {
                emailAddress = textOrNull(root, "emailAddress");
            }

            Optional<User> user = Optional.empty();
            if (emailAddress != null && !emailAddress.isBlank()) {
                user = userRepository.findByEmail(emailAddress);
            }
            if (user.isPresent()) {
                String payload = historyId != null
                        ? "{\"historyId\":\"" + historyId + "\"}"
                        : null;
                backgroundJobService.enqueue(user.get().getId(), BackgroundJob.Type.INCREMENTAL_SYNC, payload);
                log.info("Enqueued INCREMENTAL_SYNC for user {}", user.get().getId());
            } else {
                log.info("Gmail push: no matching user for email={} historyId={}", emailAddress, historyId);
            }
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (Exception e) {
            log.warn("Gmail push parse failed: {}", e.getMessage());
            // Still 200 so Pub/Sub does not retry endlessly on malformed payloads
            return ResponseEntity.ok(Map.of("status", "error"));
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text != null && !text.isBlank() ? text : null;
    }
}
