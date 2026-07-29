package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NexoraApplication {

    /**
     * Loads .env for local development, as a FALLBACK only.
     *
     * This used to call System.setProperty unconditionally. JVM system
     * properties outrank environment variables in Spring's resolution order,
     * so the file silently beat every real environment variable — the inverse
     * of how dotenv is meant to work. Exporting DEV_BYPASS_ENABLED=true had no
     * effect while .env said false, and on a deployed instance a stray .env
     * would quietly override the platform's injected configuration, including
     * secrets. Skipping keys that are already set restores the normal
     * precedence: real environment beats the file.
     */
    static {
        try {
            java.nio.file.Path path = java.nio.file.Paths.get(".env");
            if (!java.nio.file.Files.exists(path)) {
                // Fallback to parent directory or config dir if not found in root (e.g. running from parent folder)
                path = java.nio.file.Paths.get("backend/.env");
            }
            if (java.nio.file.Files.exists(path)) {
                java.nio.file.Files.lines(path).forEach(line -> {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int eqIdx = trimmed.indexOf("=");
                        String key = trimmed.substring(0, eqIdx).trim();
                        String val = trimmed.substring(eqIdx + 1).trim();
                        if ((val.startsWith("\"") && val.endsWith("\"")) || (val.startsWith("'") && val.endsWith("'"))) {
                            val = val.substring(1, val.length() - 1);
                        }
                        if (key.isEmpty()) return;
                        // A real environment variable, or a -D passed on the
                        // command line, always wins over the file.
                        if (System.getenv(key) != null) return;
                        if (System.getProperty(key) != null) return;
                        System.setProperty(key, val);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to load .env configuration: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(NexoraApplication.class, args);
    }
}
