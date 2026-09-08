package com.nexora.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fast in-memory JWT revoke gate. Durable enforcement compares JWT {@code tv}
 * to {@code users.token_version} in {@link JwtAuthenticationFilter}.
 */
@Component
public class JwtRevocationRegistry {

    private final ConcurrentHashMap<Long, Integer> minAcceptedVersion = new ConcurrentHashMap<>();

    public void revokeAtLeast(long userId, int tokenVersion) {
        minAcceptedVersion.merge(userId, tokenVersion, Math::max);
    }

    public boolean isRevoked(long userId, int jwtTokenVersion) {
        Integer min = minAcceptedVersion.get(userId);
        return min != null && jwtTokenVersion < min;
    }
}
