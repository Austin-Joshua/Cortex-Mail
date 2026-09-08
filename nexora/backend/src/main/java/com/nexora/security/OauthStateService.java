package com.nexora.security;

import com.nexora.model.OauthExchangeCode;
import com.nexora.repository.OauthExchangeCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * One-time OAuth {@code state} values to block login CSRF.
 * Stored in {@code oauth_exchange_codes} so multi-instance deploys share state.
 */
@Service
@RequiredArgsConstructor
public class OauthStateService {

    private static final String PREFIX = "s:";
    private static final String MARKER = "oauth_state";
    private static final int TTL_MINUTES = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OauthExchangeCodeRepository repository;
    private final TokenEncryptor tokenEncryptor;

    @Transactional
    public String issue() {
        repository.deleteExpired(LocalDateTime.now());
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        repository.save(OauthExchangeCode.builder()
                .code(PREFIX + state)
                .payload(tokenEncryptor.encrypt(MARKER))
                .expiresAt(LocalDateTime.now().plusMinutes(TTL_MINUTES))
                .build());
        return state;
    }

    @Transactional
    public boolean consume(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Optional<OauthExchangeCode> row = repository.findById(PREFIX + state.trim());
        if (row.isEmpty()) {
            return false;
        }
        OauthExchangeCode exchange = row.get();
        repository.delete(exchange);
        if (exchange.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }
        try {
            return MARKER.equals(tokenEncryptor.decrypt(exchange.getPayload()));
        } catch (Exception e) {
            return false;
        }
    }
}
