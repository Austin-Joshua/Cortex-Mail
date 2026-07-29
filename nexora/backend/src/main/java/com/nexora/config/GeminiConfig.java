package com.nexora.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.base-url}")
    private String baseUrl;

    public String getApiKey() {
        return apiKey;
    }

    public String getModel() {
        return model;
    }

    /**
     * Endpoint for a generateContent call. The key is deliberately NOT in the
     * query string — API keys in URLs end up in access logs, proxy logs and
     * browser history. Callers must send it as the x-goog-api-key header.
     */
    public String getGenerateContentUrl() {
        return baseUrl + "/" + model + ":generateContent";
    }

    public boolean isConfigured() {
        return apiKey != null
                && !apiKey.isBlank()
                && !apiKey.contains("mock")
                && !apiKey.contains("your_")
                && !apiKey.contains("your-");
    }

    /** Never log the key itself — only whether one is present. */
    public String describe() {
        return isConfigured()
                ? "Gemini configured (model=" + model + ", key=***" + apiKey.substring(Math.max(0, apiKey.length() - 4)) + ")"
                : "Gemini not configured — falling back to local classification";
    }
}
