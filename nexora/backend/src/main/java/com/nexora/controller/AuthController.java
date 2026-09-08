package com.nexora.controller;

import com.nexora.dto.request.ProfileUpdateRequest;
import com.nexora.dto.response.AuthResponse;
import com.nexora.security.AuthCookieService;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.CookieNames;
import com.nexora.security.JwtTokenProvider;
import com.nexora.security.OauthStateService;
import com.nexora.security.UserPrincipal;
import com.nexora.service.AuthService;
import com.nexora.service.GmailWatchService;
import com.nexora.service.OauthExchangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OauthExchangeService oauthExchangeService;
    private final OauthStateService oauthStateService;
    private final AuthCookieService authCookieService;
    private final GmailWatchService gmailWatchService;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.cors-allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Issue a short-lived OAuth {@code state} for the SPA to attach to Google's authorize URL.
     */
    @GetMapping("/oauth/state")
    public ResponseEntity<Map<String, String>> issueOauthState() {
        return ResponseEntity.ok(Map.of("state", oauthStateService.issue()));
    }

    /**
     * Frontend redirects user to Google with {@code state}; Google redirects here with ?code=&state=.
     * We exchange the code, register/load the user, and redirect back to the React app callback page.
     */
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        String frontendBase = corsAllowedOrigins.split(",")[0].trim();

        if (error != null && !error.isBlank()) {
            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/")
                    .queryParam("auth_error", error)
                    .queryParam("error_description", errorDescription != null ? errorDescription : "")
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
            return;
        }

        if (!oauthStateService.consume(state)) {
            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/")
                    .queryParam("auth_error", "invalid_state")
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
            return;
        }

        if (code == null || code.isBlank()) {
            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/")
                    .queryParam("auth_error", "missing_code")
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
            return;
        }

        try {
            AuthResponse authResponse = authService.handleGoogleCallback(code);

            String exchangeCode = oauthExchangeService.store(
                    authResponse.getUserId(), authResponse.isOnboardingComplete());

            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/auth/callback")
                    .queryParam("code", exchangeCode)
                    .build().toUriString();

            response.sendRedirect(redirectUrl);
        } catch (Exception ex) {
            log.error("OAuth callback failed: {}", ex.getMessage());
            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/")
                    .queryParam("auth_error", "oauth_failed")
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
        }
    }

    /**
     * Initiate Google OAuth — redirect to Google's consent screen.
     */
    @GetMapping("/google")
    public ResponseEntity<Void> initiateGoogleAuth() {
        // This endpoint is documented but the actual redirect URL is constructed by the frontend
        return ResponseEntity.ok().build();
    }

    @GetMapping("/token")
    public ResponseEntity<AuthResponse> exchangeCode(
            @RequestParam String code,
            HttpServletRequest request,
            HttpServletResponse response) {
        return oauthExchangeService.consume(code)
                .map(payload -> {
                    AuthResponse auth = authService.issueSession(
                            payload.userId(), payload.onboardingComplete());
                    setSessionCookies(request, response, auth);
                    try {
                        gmailWatchService.setupWatch(auth.getUserId());
                    } catch (Exception e) {
                        log.warn("Watch setup after login failed for user {}: {}",
                                auth.getUserId(), e.getMessage());
                    }
                    return ResponseEntity.ok(auth);
                })
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response) {
        String refresh = authCookieService.readCookie(request, com.nexora.security.CookieNames.REFRESH_TOKEN);
        if (refresh == null || refresh.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        try {
            AuthResponse auth = authService.refreshSession(refresh);
            setSessionCookies(request, response, auth);
            return ResponseEntity.ok(auth);
        } catch (Exception e) {
            authCookieService.clearAuthCookies(request, response);
            return ResponseEntity.status(401).build();
        }
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(authService.getProfile(AuthPrincipals.requireId(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProfileUpdateRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        AuthResponse response = authService.updateProfile(AuthPrincipals.requireId(user), request);
        if (response.getToken() != null) {
            setSessionCookies(httpRequest, httpResponse, response);
        }
        return ResponseEntity.ok(response);
    }

    /** Soft logout — invalidate JWTs; keep Gmail connection for next sign-in. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserPrincipal user,
            HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = resolveSessionUserId(user, request);
        if (userId != null) {
            authService.logout(userId);
        }
        authCookieService.clearAuthCookies(request, response);
        return ResponseEntity.ok().build();
    }

    /** Hard disconnect — wipe Gmail tokens and invalidate JWTs. */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(
            @AuthenticationPrincipal UserPrincipal user,
            HttpServletRequest request,
            HttpServletResponse response) {
        Long userId = resolveSessionUserId(user, request);
        if (userId != null) {
            authService.revokeAccess(userId);
        }
        authCookieService.clearAuthCookies(request, response);
        return ResponseEntity.ok().build();
    }

    /**
     * Prefer live access principal; fall back to refresh cookie so logout still works
     * when the access token has expired.
     */
    private Long resolveSessionUserId(UserPrincipal user, HttpServletRequest request) {
        if (user != null && user.getId() != null) {
            return user.getId();
        }
        String refresh = authCookieService.readCookie(request, CookieNames.REFRESH_TOKEN);
        if (refresh == null || refresh.isBlank()) {
            return null;
        }
        try {
            if (jwtTokenProvider.validateToken(refresh) && jwtTokenProvider.isRefreshToken(refresh)) {
                return jwtTokenProvider.getUserIdFromToken(refresh);
            }
        } catch (Exception e) {
            log.debug("Refresh cookie unusable for logout/revoke: {}", e.getMessage());
        }
        return null;
    }

    private void setSessionCookies(HttpServletRequest request, HttpServletResponse response, AuthResponse auth) {
        if (auth == null || auth.getToken() == null) {
            return;
        }
        String refresh = authService.issueRefreshToken(auth.getUserId());
        authCookieService.setAuthCookies(request, response, auth.getToken(), refresh);
    }
}
