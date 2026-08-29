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
        return ResponseEntity.ok(Map.of(
            "configured", geminiConfig.isConfigured(),
            "model", geminiConfig.getModel(),
            "refinementBatchSize", geminiConfig.getRefinementBatchSize(),
            "maxConcurrent", geminiConfig.getMaxConcurrent()
        ));
    }
}
