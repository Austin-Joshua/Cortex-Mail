package com.nexora.security;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One-time OAuth {@code state} values to block login CSRF.
 * In-memory is enough for single-instance local/prod; multi-instance needs a shared store.
 */
@Component
public class OauthStateService {

    private static final long TTL_MS = 10 * 60 * 1000L;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ConcurrentHashMap<String, Long> pending = new ConcurrentHashMap<>();

    public String issue() {
        purgeExpired();
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        pending.put(state, System.currentTimeMillis() + TTL_MS);
        return state;
    }

    public boolean consume(String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        Long expiry = pending.remove(state);
        return expiry != null && expiry >= System.currentTimeMillis();
    }

    private void purgeExpired() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<String, Long>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Long> e = it.next();
            if (e.getValue() < now) {
                it.remove();
            }
        }
    }
}
