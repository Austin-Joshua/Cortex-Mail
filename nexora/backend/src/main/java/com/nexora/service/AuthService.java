package com.nexora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.dto.response.AuthResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.User;
import com.nexora.model.User.UserRole;
import com.nexora.repository.UserRepository;
import com.nexora.security.JwtRevocationRegistry;
import com.nexora.security.JwtTokenProvider;
import com.nexora.security.TokenEncryptor;
import com.nexora.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Objects;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenEncryptor tokenEncryptor;
    private final ObjectMapper objectMapper;
    private final JwtRevocationRegistry jwtRevocationRegistry;

    @Value("${google.client-id}")
    private String googleClientId;

    @Value("${google.client-secret}")
    private String googleClientSecret;

    @Value("${google.redirect-uri}")
    private String redirectUri;

    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://www.googleapis.com/oauth2/v3/userinfo";

    /**
     * Exchange Google OAuth code for tokens, upsert user, return JWT.
     * Always uses the configured {@code GOOGLE_REDIRECT_URI} (never Host-header derived).
     */
    public AuthResponse handleGoogleCallback(String code) {
        // 1. Exchange code for tokens
        JsonNode tokenResponse = exchangeCodeForTokens(code);

        String accessToken  = tokenResponse.get("access_token").asText();
        String refreshToken = tokenResponse.has("refresh_token")
                ? tokenResponse.get("refresh_token").asText() : null;
        long expiresIn      = tokenResponse.has("expires_in")
                ? tokenResponse.get("expires_in").asLong() : 3600L;

        // 2. Fetch Google user profile
        JsonNode userInfo = fetchUserInfo(accessToken);
        String googleId  = userInfo.get("sub").asText();
        String email     = userInfo.get("email").asText();
        String name      = userInfo.has("name") ? userInfo.get("name").asText() : email;
        String picture   = userInfo.has("picture") ? userInfo.get("picture").asText() : null;

        // 3. Upsert user in DB with encrypted tokens
        LocalDateTime expiry = LocalDateTime.now().plusSeconds(expiresIn);
        User user = userRepository.findByGoogleId(googleId).orElse(null);
        boolean isNew = user == null;

        if (isNew) {
            user = User.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .profilePictureUrl(picture)
                    .userRole(UserRole.STUDENT)
                    .gmailAccessToken(tokenEncryptor.encrypt(accessToken))
                    .gmailRefreshToken(refreshToken != null ? tokenEncryptor.encrypt(refreshToken) : null)
                    .tokenExpiry(expiry)
                    .build();
        } else {
            // user is non-null here: findByGoogleId returned a present Optional
            Objects.requireNonNull(user, "Existing user must not be null");
            user.setName(name);
            user.setProfilePictureUrl(picture);
            user.setGmailAccessToken(tokenEncryptor.encrypt(accessToken));
            if (refreshToken != null) {
                user.setGmailRefreshToken(tokenEncryptor.encrypt(refreshToken));
            }
            user.setTokenExpiry(expiry);
        }
        user = userRepository.save(user);

        // 4. Issue JWT
        String jwt = jwtTokenProvider.generateToken(user);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .userRole(user.getUserRole())
                // Common app: no profession onboarding; welcome screen optional via updateProfile.
                .onboardingComplete(!isNew)
                .calendarSyncEnabled(user.getCalendarSyncEnabled())
                .lastSyncedAt(user.getLastSyncedAt())
                .build();
    }

    public User getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NexoraException("User not found", 404));
    }

    public AuthResponse buildAuthResponse(String token, boolean onboardingComplete) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        User user = getCurrentUser(userId);
        return toAuthResponse(user, token, onboardingComplete);
    }

    /** Issue a fresh JWT after opaque OAuth code exchange — JWT never touched the DB. */
    public AuthResponse issueSession(long userId, boolean onboardingComplete) {
        User user = getCurrentUser(userId);
        String jwt = jwtTokenProvider.generateToken(user);
        return toAuthResponse(user, jwt, onboardingComplete);
    }

    public String issueRefreshToken(Long userId) {
        User user = getCurrentUser(userId);
        return jwtTokenProvider.generateRefreshToken(user);
    }

    /** Rotate access + refresh from a valid refresh JWT (typ=refresh, token_version bound). */
    public AuthResponse refreshSession(String refreshToken) {
        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)
                || !jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new NexoraException("Invalid refresh token", 401);
        }
        UserPrincipal principal = jwtTokenProvider.toPrincipal(refreshToken);
        User user = getCurrentUser(principal.getId());
        int dbVersion = user.getTokenVersion() != null ? user.getTokenVersion() : 0;
        if (principal.getTokenVersion() < dbVersion) {
            throw new NexoraException("Refresh token revoked", 401);
        }
        String access = jwtTokenProvider.generateToken(user);
        return toAuthResponse(user, access, true);
    }

    /** Read-only profile for /me — no DB write and no JWT rotation. */
    public AuthResponse getProfile(Long userId) {
        User user = getCurrentUser(userId);
        return toAuthResponse(user, null, true);
    }

    public AuthResponse updateProfile(Long userId, com.nexora.dto.request.ProfileUpdateRequest request) {
        User user = getCurrentUser(userId);
        if (request.getUserRole() != null) {
            // Stored for JWT compatibility only — classification is mailbox-personalized, not role-driven.
            user.setUserRole(request.getUserRole());
        }
        if (request.getCalendarSyncEnabled() != null) {
            user.setCalendarSyncEnabled(request.getCalendarSyncEnabled());
        }
        if (request.getQuietHoursStart() != null) {
            // Negative sentinel clears quiet-hours start (SPA sends -1 for blank).
            user.setQuietHoursStart(request.getQuietHoursStart() < 0 ? null : clampHour(request.getQuietHoursStart()));
        }
        if (request.getQuietHoursEnd() != null) {
            user.setQuietHoursEnd(request.getQuietHoursEnd() < 0 ? null : clampHour(request.getQuietHoursEnd()));
        }
        if (request.getMutedCategories() != null) {
            user.setMutedCategories(request.getMutedCategories().isBlank() ? null : request.getMutedCategories().trim());
        }
        if (request.getDigestEnabled() != null) {
            user.setDigestEnabled(request.getDigestEnabled());
        }
        if (request.getDigestHour() != null) {
            user.setDigestHour(clampHour(request.getDigestHour()));
        }
        user = userRepository.save(user);
        String jwt = jwtTokenProvider.generateToken(user);
        return toAuthResponse(user, jwt, true);
    }

    /** @deprecated Prefer {@link #updateProfile(Long, com.nexora.dto.request.ProfileUpdateRequest)}. */
    @Deprecated
    public AuthResponse updateProfile(Long userId, UserRole role, Boolean calendarSyncEnabled) {
        com.nexora.dto.request.ProfileUpdateRequest request = new com.nexora.dto.request.ProfileUpdateRequest();
        request.setUserRole(role);
        request.setCalendarSyncEnabled(calendarSyncEnabled);
        return updateProfile(userId, request);
    }

    private static Integer clampHour(Integer hour) {
        if (hour == null) {
            return null;
        }
        return Math.max(0, Math.min(23, hour));
    }

    private AuthResponse toAuthResponse(User user, String token, boolean onboardingComplete) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .userRole(user.getUserRole())
                .onboardingComplete(onboardingComplete)
                .calendarSyncEnabled(user.getCalendarSyncEnabled())
                .lastSyncedAt(user.getLastSyncedAt())
                .quietHoursStart(user.getQuietHoursStart())
                .quietHoursEnd(user.getQuietHoursEnd())
                .mutedCategories(user.getMutedCategories())
                .digestEnabled(user.getDigestEnabled())
                .digestHour(user.getDigestHour())
                .build();
    }

    /** Invalidate app JWTs without disconnecting Gmail. */
    public void logout(Long userId) {
        bumpTokenVersion(userId);
    }

    /** Disconnect Gmail tokens and invalidate outstanding JWTs. */
    public void revokeAccess(Long userId) {
        User user = getCurrentUser(userId);
        user.setGmailAccessToken(null);
        user.setGmailRefreshToken(null);
        user.setTokenExpiry(null);
        int next = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(next);
        userRepository.save(user);
        jwtRevocationRegistry.revokeAtLeast(userId, next);
    }

    private void bumpTokenVersion(Long userId) {
        User user = getCurrentUser(userId);
        int next = (user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1;
        user.setTokenVersion(next);
        userRepository.save(user);
        jwtRevocationRegistry.revokeAtLeast(userId, next);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private JsonNode exchangeCodeForTokens(String code) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        if (redirectUri == null || redirectUri.isBlank()) {
            throw new NexoraException("GOOGLE_REDIRECT_URI is not configured", 500);
        }
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_ENDPOINT, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Failed to exchange code for tokens. Status: {}", e.getStatusCode());
            throw new NexoraException("Failed to authenticate with Google", 401);
        } catch (Exception e) {
            log.error("Failed to exchange code for tokens: {}", e.getMessage());
            throw new NexoraException("Failed to authenticate with Google", 401);
        }
    }

    private JsonNode fetchUserInfo(String accessToken) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    USERINFO_ENDPOINT, HttpMethod.GET, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (Exception e) {
            log.error("Failed to fetch user info: {}", e.getMessage());
            throw new NexoraException("Failed to fetch Google profile", 401);
        }
    }

}
