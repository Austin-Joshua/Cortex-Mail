package com.nexora.controller;

import com.nexora.dto.request.ProfileUpdateRequest;
import com.nexora.dto.response.AuthResponse;
import com.nexora.model.User;
import com.nexora.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${app.cors-allowed-origins}")
    private String corsAllowedOrigins;

    private final java.util.Map<String, String> pendingTokens = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Frontend redirects user to:
     * https://accounts.google.com/o/oauth2/v2/auth?client_id=...&redirect_uri=.../api/auth/google/callback&response_type=code&scope=...&access_type=offline&prompt=consent
     *
     * Google then redirects back here with ?code=...
     * We exchange the code, register/load the user, and redirect back to the React app callback page.
     */
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam String code,
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        
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
        String frontendBase = corsAllowedOrigins.split(",")[0].trim();

        String exchangeCode = java.util.UUID.randomUUID().toString();
        String valueToStore = authResponse.getToken() + ";" + authResponse.isOnboardingComplete();
        pendingTokens.put(exchangeCode, valueToStore);

        // Auto-expire after 60 seconds
        java.util.concurrent.CompletableFuture.delayedExecutor(60, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> pendingTokens.remove(exchangeCode));

        String redirectUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(frontendBase + "/auth/callback")
                .queryParam("code", exchangeCode)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    @Value("${google.client-id}")
    private String googleClientId;

    /** Scopes requested at consent. Read-only on mail; calendar write is for deadlines. */
    private static final String GOOGLE_SCOPES = String.join(" ",
            "https://www.googleapis.com/auth/gmail.readonly",
            "https://www.googleapis.com/auth/calendar.events",
            "openid", "email", "profile");

    /**
     * Starts Google OAuth by redirecting to the consent screen.
     *
     * The URL is built here rather than in the browser on purpose. When the
     * frontend assembled it, the client id had to be duplicated into
     * VITE_GOOGLE_CLIENT_ID; if that was unset the button sent users to
     * Google with `client_id=` empty and they got a raw "Error 400:
     * invalid_request" page with nothing pointing back at the real cause.
     * One source of truth, and a legible error when it is missing.
     */
    @GetMapping("/google")
    public void initiateGoogleAuth(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {

        if (googleClientId == null || googleClientId.isBlank() || googleClientId.contains("your_")) {
            response.sendError(jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                    "Google sign-in is not configured on the server: GOOGLE_CLIENT_ID is unset.");
            return;
        }

        String redirectUri = publicBaseUrl(request) + "/api/auth/google/callback";

        String authUrl = org.springframework.web.util.UriComponentsBuilder
                .fromHttpUrl("https://accounts.google.com/o/oauth2/v2/auth")
                .queryParam("client_id", googleClientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", GOOGLE_SCOPES)
                .queryParam("access_type", "offline")
                .queryParam("prompt", "consent")
                .encode()
                .build()
                .toUriString();

        response.sendRedirect(authUrl);
    }

    /**
     * Public origin of this server, honouring reverse-proxy headers. Render and
     * similar hosts terminate TLS upstream, so request.getScheme() reports http
     * and a redirect_uri built from it would not match the one registered with
     * Google.
     */
    private String publicBaseUrl(jakarta.servlet.http.HttpServletRequest request) {
        String scheme = request.getHeader("X-Forwarded-Proto");
        if (scheme != null && !scheme.isBlank()) {
            scheme = scheme.split(",")[0].trim();
        } else {
            scheme = request.getScheme();
        }

        String host = request.getHeader("X-Forwarded-Host");
        if (host != null && !host.isBlank()) {
            host = host.split(",")[0].trim();
        } else {
            host = request.getHeader("Host");
        }
        if (host == null || host.isBlank()) {
            int port = request.getServerPort();
            host = request.getServerName() + (port == 80 || port == 443 ? "" : ":" + port);
        }
        return scheme + "://" + host;
    }

    /**
     * Developer bypass to log in instantly with mock data.
     */
    @GetMapping("/bypass")
    public void developerBypass(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        AuthResponse authResponse = authService.handleBypassLogin();
        String frontendBase = corsAllowedOrigins.split(",")[0].trim();

        String exchangeCode = java.util.UUID.randomUUID().toString();
        String valueToStore = authResponse.getToken() + ";" + authResponse.isOnboardingComplete();
        pendingTokens.put(exchangeCode, valueToStore);

        // Auto-expire after 60 seconds
        java.util.concurrent.CompletableFuture.delayedExecutor(60, java.util.concurrent.TimeUnit.SECONDS)
                .execute(() -> pendingTokens.remove(exchangeCode));

        String redirectUrl = org.springframework.web.util.UriComponentsBuilder.fromHttpUrl(frontendBase + "/auth/callback")
                .queryParam("code", exchangeCode)
                .build().toUriString();

        response.sendRedirect(redirectUrl);
    }

    @GetMapping("/token")
    public ResponseEntity<AuthResponse> exchangeCode(@RequestParam String code) {
        String value = pendingTokens.remove(code);
        if (value == null) {
            return ResponseEntity.status(401).build();
        }
        String[] parts = value.split(";");
        String jwt = parts[0];
        boolean onboardingComplete = Boolean.parseBoolean(parts[1]);
        return ResponseEntity.ok(authService.buildAuthResponse(jwt, onboardingComplete));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> getCurrentUser(@AuthenticationPrincipal User user) {
        AuthResponse response = authService.updateProfile(user.getId(), null, null);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequest request) {
        AuthResponse response = authService.updateProfile(user.getId(), request.getUserRole(), request.getCalendarSyncEnabled());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revokeAccess(@AuthenticationPrincipal User user) {
        authService.revokeAccess(user.getId());
        return ResponseEntity.ok().build();
    }
}
