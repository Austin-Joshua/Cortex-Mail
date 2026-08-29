package com.nexora;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class NexoraApplication {

    private static Map<String, Object> loadDotEnv() {
        Map<String, Object> props = new HashMap<>();
        try {
            Path path = Paths.get(".env");
            if (!Files.exists(path)) {
                path = Paths.get("backend/.env");
            }
            if (!Files.exists(path)) {
                path = Paths.get("nexora/backend/.env");
            }
            if (!Files.exists(path)) {
                return props;
            }
            try (Stream<String> lines = Files.lines(path)) {
                lines.forEach(line -> {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                        return;
                    }
                    int eqIdx = trimmed.indexOf('=');
                    String key = trimmed.substring(0, eqIdx).trim();
                    String val = trimmed.substring(eqIdx + 1).trim();
                    if ((val.startsWith("\"") && val.endsWith("\""))
                            || (val.startsWith("'") && val.endsWith("'"))) {
                        val = val.substring(1, val.length() - 1);
                    }
                    if ("SPRING_PROFILES_ACTIVE".equals(key)) {
                        System.setProperty("spring.profiles.active", val);
                    }
                    System.setProperty(key, val);
                    props.put(key, val);
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to load .env configuration: " + e.getMessage());
        }
        return props;
    }

    public static void main(String[] args) {
        Map<String, Object> envDefaults = loadDotEnv();
        SpringApplication app = new SpringApplication(NexoraApplication.class);
        if (!envDefaults.isEmpty()) {
            app.setDefaultProperties(envDefaults);
        }
        app.run(args);
    }
}
