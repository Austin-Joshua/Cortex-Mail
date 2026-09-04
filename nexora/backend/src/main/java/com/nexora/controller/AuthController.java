package com.nexora.controller;

import com.nexora.dto.request.ProfileUpdateRequest;
import com.nexora.dto.response.AuthResponse;
import com.nexora.security.AuthPrincipals;
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

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final OauthExchangeService oauthExchangeService;

    @Value("${app.cors-allowed-origins}")
    private String corsAllowedOrigins;

    /**
     * Frontend redirects user to:
     * https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=.../api/auth/google/callback&response_type=code&scope=...&access_type=offline&prompt=consent
     *
     * Google then redirects back here with ?code=...
     * We exchange the code, register/load the user, and redirect back to the React app callback page.
     */
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error,
            @RequestParam(name = "error_description", required = false) String errorDescription,
            jakarta.servlet.http.HttpServletRequest request,
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

        if (code == null || code.isBlank()) {
            String redirectUrl = org.springframework.web.util.UriComponentsBuilder
                    .fromHttpUrl(frontendBase + "/")
                    .queryParam("auth_error", "missing_code")
                    .build().toUriString();
            response.sendRedirect(redirectUrl);
            return;
        }

        try {
            // Dynamically detect scheme, taking reverse proxies into account
            String scheme = request.getHeader("X-Forwarded-Proto");
            if (scheme != null && !scheme.isEmpty()) {
                scheme = scheme.split(",")[0].trim();
            } else {
                scheme = request.getScheme();
            }

            // Dynamically detect host, taking reverse proxies into account
            String host = request.getHeader("X-Forwarded-Host");
            if (host != null && !host.isEmpty()) {
                host = host.split(",")[0].trim();
            } else {
                host = request.getHeader("Host");
            }
            if (host == null || host.isEmpty()) {
                int port = request.getServerPort();
                host = request.getServerName() + (port == 80 || port == 443 ? "" : ":" + port);
            }

            String dynamicRedirectUri = scheme + "://" + host + request.getRequestURI();

            AuthResponse authResponse = authService.handleGoogleCallback(code, dynamicRedirectUri);

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

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(@AuthenticationPrincipal UserPrincipal user) {
        authService.revokeAccess(AuthPrincipals.requireId(user));
        return ResponseEntity.ok().build();
    }
}
