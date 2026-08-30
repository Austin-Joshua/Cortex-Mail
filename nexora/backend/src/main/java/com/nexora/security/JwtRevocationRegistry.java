package com.nexora.security;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory JWT revoke gate. On logout / revokeAccess we bump the user's
 * token_version and publish it here so outstanding JWTs fail without a DB hit
 * on every request. Survives for the life of the JVM (single-instance deploy).
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
