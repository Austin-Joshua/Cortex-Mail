package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NexoraApplication {

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
                        // Spring Boot reads spring.profiles.active; map common env alias.
                        if ("SPRING_PROFILES_ACTIVE".equals(key)) {
                            System.setProperty("spring.profiles.active", val);
                        }
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
