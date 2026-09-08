package com.nexora.controller;

import com.nexora.dto.request.ProfileUpdateRequest;
import com.nexora.dto.response.AuthResponse;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.OauthStateService;
import com.nexora.security.UserPrincipal;
import com.nexora.service.AuthService;
import com.nexora.service.OauthExchangeService;
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
    public ResponseEntity<AuthResponse> exchangeCode(@RequestParam String code) {
        return oauthExchangeService.consume(code)
                .map(payload -> ResponseEntity.ok(
                        authService.issueSession(payload.userId(), payload.onboardingComplete())))
                .orElseGet(() -> ResponseEntity.status(401).build());
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(authService.getProfile(AuthPrincipals.requireId(user)));
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        AuthResponse response = authService.updateProfile(AuthPrincipals.requireId(user), request.getUserRole(), request.getCalendarSyncEnabled());
        return ResponseEntity.ok(response);
    }

    /** Soft logout — invalidate JWTs; keep Gmail connection for next sign-in. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal UserPrincipal user) {
        authService.logout(AuthPrincipals.requireId(user));
        return ResponseEntity.ok().build();
    }

    /** Hard disconnect — wipe Gmail tokens and invalidate JWTs. */
    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(@AuthenticationPrincipal UserPrincipal user) {
        authService.revokeAccess(AuthPrincipals.requireId(user));
        return ResponseEntity.ok().build();
    }
}
