package com.nexora.controller;

import com.nexora.config.GeminiConfig;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class GeminiStatusController {

    private final GeminiConfig geminiConfig;

    @GetMapping("/gemini-status")
    public ResponseEntity<Map<String, Object>> getGeminiStatus(
            @AuthenticationPrincipal UserPrincipal user) {
        AuthPrincipals.requireId(user);
        return ResponseEntity.ok(Map.of(
            "configured", geminiConfig.isConfigured(),
            "mode", geminiConfig.isConfigured() ? "gemini" : "rules"
        ));
    }
}
