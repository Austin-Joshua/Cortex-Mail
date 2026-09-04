package com.nexora.security;

import com.nexora.exception.NexoraException;

/**
 * Rejects a missing JWT principal before any mailbox work runs.
 */
public final class AuthPrincipals {

    private AuthPrincipals() {}

    public static Long requireId(UserPrincipal user) {
        if (user == null || user.getId() == null) {
            throw new NexoraException("Unauthorized", 401);
        }
        return user.getId();
    }
}
