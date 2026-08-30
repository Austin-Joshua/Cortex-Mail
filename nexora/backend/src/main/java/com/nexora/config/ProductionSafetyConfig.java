package com.nexora.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("prod")
@RequiredArgsConstructor
public class ProductionSafetyConfig {

    private final Environment environment;

    @PostConstruct
    void failFastOnUnsafeProdConfig() {
        String url = environment.getProperty("spring.datasource.url", "");
        if (url.isBlank() || url.toLowerCase().contains("jdbc:h2")) {
            throw new IllegalStateException("Production profile must use Postgres. Set DB_URL.");
        }

        require("jwt.secret", "JWT_SECRET", 32);
        require("app.encryption-key", "ENCRYPTION_KEY", 16);
        requireNonBlank("google.client-id", "GOOGLE_CLIENT_ID");
        requireNonBlank("google.client-secret", "GOOGLE_CLIENT_SECRET");
        requireNonBlank("google.redirect-uri", "GOOGLE_REDIRECT_URI");
    }

    private void require(String property, String envHint, int minBytes) {
        String value = environment.getProperty(property, "");
        if (value.isBlank() || value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < minBytes) {
            throw new IllegalStateException(
                    property + " must be set in production (env " + envHint + ", min " + minBytes + " bytes).");
        }
    }

    private void requireNonBlank(String property, String envHint) {
        String value = environment.getProperty(property, "");
        if (value.isBlank() || value.contains("your-") || value.contains("your_")) {
            throw new IllegalStateException(property + " must be configured in production (env " + envHint + ").");
        }
    }
}
