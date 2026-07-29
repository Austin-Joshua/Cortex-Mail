package com.nexora.controller;

import com.nexora.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> getGeminiStatus() {
        // Report the model actually in use. This was hardcoded to
        // "gemini-1.5-flash" and kept saying so long after the configured
        // model changed — a status endpoint that reports a constant tells
        // you nothing about the thing it claims to be reporting on.
        return ResponseEntity.ok(Map.of(
            "configured", geminiConfig.isConfigured(),
            "model", geminiConfig.getModel()
        ));
    }
}
