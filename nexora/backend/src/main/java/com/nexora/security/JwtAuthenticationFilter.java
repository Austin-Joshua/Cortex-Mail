package com.nexora.security;

import com.nexora.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtRevocationRegistry revocationRegistry;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)
                && jwtTokenProvider.isAccessToken(token)) {
            try {
                UserPrincipal principal = jwtTokenProvider.toPrincipal(token);
                if (principal != null && isSessionActive(principal)) {
                    var authority = new SimpleGrantedAuthority("ROLE_" + principal.getUserRole().name());
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(authority));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                log.error("Could not set user authentication: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Fast in-memory revoke gate plus durable DB {@code token_version} check so
     * logout survives process restarts (and stays correct for single-instance deploys).
     */
    private boolean isSessionActive(UserPrincipal principal) {
        int jwtVersion = principal.getTokenVersion();
        if (revocationRegistry.isRevoked(principal.getId(), jwtVersion)) {
            return false;
        }
        Integer dbVersion = userRepository.findTokenVersionById(principal.getId()).orElse(null);
        if (dbVersion == null) {
            return false;
        }
        revocationRegistry.revokeAtLeast(principal.getId(), dbVersion);
        return jwtVersion >= dbVersion;
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        if (request.getCookies() != null) {
            for (jakarta.servlet.http.Cookie cookie : request.getCookies()) {
                if (CookieNames.ACCESS_TOKEN.equals(cookie.getName())
                        && StringUtils.hasText(cookie.getValue())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
