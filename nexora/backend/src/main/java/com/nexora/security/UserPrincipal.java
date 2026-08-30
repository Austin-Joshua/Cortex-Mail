package com.nexora.security;

import com.nexora.model.User.UserRole;
import lombok.Getter;

/**
 * Lightweight auth principal built from JWT claims — no DB round-trip per request.
 * Services that need Gmail tokens / mutable profile fields must load {@link com.nexora.model.User} by id.
 */
@Getter
public final class UserPrincipal {

    private final Long id;
    private final String email;
    private final String name;
    private final UserRole userRole;
    private final int tokenVersion;

    public UserPrincipal(Long id, String email, String name, UserRole userRole) {
        this(id, email, name, userRole, 0);
    }

    public UserPrincipal(Long id, String email, String name, UserRole userRole, int tokenVersion) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.userRole = userRole != null ? userRole : UserRole.STUDENT;
        this.tokenVersion = tokenVersion;
    }
}
