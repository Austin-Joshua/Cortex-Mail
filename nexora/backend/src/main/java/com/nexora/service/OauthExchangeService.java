package com.nexora.service;

import com.nexora.model.OauthExchangeCode;
import com.nexora.repository.OauthExchangeCodeRepository;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * One-time opaque OAuth handoff codes. The DB never stores a JWT —
 * only an encrypted {@code userId;onboardingComplete} payload.
 */
@Service
@RequiredArgsConstructor
public class OauthExchangeService {

    private static final int TTL_SECONDS = 60;

    private final OauthExchangeCodeRepository repository;
    private final TokenEncryptor tokenEncryptor;

    public record ExchangePayload(long userId, boolean onboardingComplete) {}

    @Transactional
    public String store(long userId, boolean onboardingComplete) {
        repository.deleteExpired(LocalDateTime.now());
        String code = UUID.randomUUID().toString().replace("-", "");
        String encrypted = tokenEncryptor.encrypt(userId + ";" + onboardingComplete);
        repository.save(OauthExchangeCode.builder()
                .code(code)
                .payload(encrypted)
                .expiresAt(LocalDateTime.now().plusSeconds(TTL_SECONDS))
                .build());
        return code;
    }

    @Transactional
    public Optional<ExchangePayload> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        Optional<OauthExchangeCode> row = repository.findById(code.trim());
        if (row.isEmpty()) {
            return Optional.empty();
        }
        OauthExchangeCode exchange = row.get();
        repository.delete(exchange);
        if (exchange.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        try {
            String plain = tokenEncryptor.decrypt(exchange.getPayload());
            String[] parts = plain.split(";", 2);
            long userId = Long.parseLong(parts[0].trim());
            boolean onboarding = parts.length > 1 && Boolean.parseBoolean(parts[1].trim());
            return Optional.of(new ExchangePayload(userId, onboarding));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
