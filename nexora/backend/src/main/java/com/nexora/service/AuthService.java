package com.nexora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexora.dto.response.AuthResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.User;
import com.nexora.model.User.UserRole;
import com.nexora.model.Email;
import com.nexora.model.EmailAction;
import com.nexora.repository.UserRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.EmailActionRepository;
import com.nexora.security.JwtTokenProvider;
import com.nexora.security.TokenEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenEncryptor tokenEncryptor;
    private final ObjectMapper objectMapper;

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
     */
    public AuthResponse handleGoogleCallback(String code, String dynamicRedirectUri) {
        // 1. Exchange code for tokens
        JsonNode tokenResponse = exchangeCodeForTokens(code, dynamicRedirectUri);

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
                .build();
    }

    public AuthResponse updateProfile(Long userId, UserRole role, Boolean calendarSyncEnabled) {
        User user = getCurrentUser(userId);
        if (role != null) {
            user.setUserRole(role);
        }
        if (calendarSyncEnabled != null) {
            user.setCalendarSyncEnabled(calendarSyncEnabled);
        }
        user = userRepository.save(user);
        String jwt = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .userRole(user.getUserRole())
                .onboardingComplete(true)
                .calendarSyncEnabled(user.getCalendarSyncEnabled())
                .lastSyncedAt(user.getLastSyncedAt())
                .build();
    }

    public void revokeAccess(Long userId) {
        User user = getCurrentUser(userId);
        user.setGmailAccessToken(null);
        user.setGmailRefreshToken(null);
        user.setTokenExpiry(null);
        userRepository.save(user);
    }

    public AuthResponse handleBypassLogin() {
        String googleId = "mock-google-id-123456";
        String email = "austinjoshuamj@gmail.com";
        String name = "Austin Joshua";
        String picture = "https://lh3.googleusercontent.com/a/default-user=s96-c";

        User user = userRepository.findByGoogleId(googleId)
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));
        if (user == null) {
            user = User.builder()
                    .googleId(googleId)
                    .email(email)
                    .name(name)
                    .profilePictureUrl(picture)
                    .userRole(UserRole.STUDENT)
                    .gmailAccessToken(tokenEncryptor.encrypt("mock-access-token"))
                    .gmailRefreshToken(tokenEncryptor.encrypt("mock-refresh-token"))
                    .tokenExpiry(LocalDateTime.now().plusHours(24))
                    .build();
        } else {
            user.setName(name);
            user.setProfilePictureUrl(picture);
            user.setGmailAccessToken(tokenEncryptor.encrypt("mock-access-token"));
            user.setTokenExpiry(LocalDateTime.now().plusHours(24));
            user.setGoogleId(googleId);
        }
        user = userRepository.save(user);

        String jwt = jwtTokenProvider.generateToken(user);
        return AuthResponse.builder()
                .token(jwt)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profilePictureUrl(user.getProfilePictureUrl())
                .userRole(user.getUserRole())
                .onboardingComplete(true)
                .calendarSyncEnabled(user.getCalendarSyncEnabled())
                .lastSyncedAt(user.getLastSyncedAt())
                .build();
    }

    private void seedMockEmails(User user) {
        // Clear existing mock emails for this user to avoid duplicating them on every login
        List<Email> existing = emailRepository.findTop20ByUserIdOrderByReceivedAtDesc(user.getId());
        if (!existing.isEmpty()) {
            emailRepository.deleteAll(existing);
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. Software Engineering Assignment
        Email assignment = Email.builder()
                .user(user)
                .gmailMessageId("msg-1")
                .gmailThreadId("thread-1")
                .senderName("Prof. Alan Turing")
                .senderEmail("turing@university.edu")
                .subject("URGENT: Software Engineering Assignment 3 Submission")
                .bodySnippet("Hi team, please ensure you submit your Software Engineering Assignment 3 by Friday evening. Late submissions will not be graded.")
                .bodyFull("Hi team, please ensure you submit your Software Engineering Assignment 3 by Friday evening. Make sure to include the architecture diagram and UML designs. Late submissions will not be graded.")
                .receivedAt(now.minusHours(4))
                .isRead(false)
                .hasAttachments(true)
                .category(Email.EmailCategory.ASSIGNMENT)
                .priority(Email.Priority.HIGH)
                .aiSummary("Submit Software Engineering Assignment 3 by Friday evening. Architecture diagram and UML designs are required.")
                .deadlineDetected(now.plusDays(2))
                .build();
        assignment = emailRepository.save(assignment);

        EmailAction assignmentAction = EmailAction.builder()
                .email(assignment)
                .userId(user.getId())
                .actionType(EmailAction.ActionType.SUBMIT)
                .actionDescription("Submit Software Engineering Assignment 3")
                .deadline(now.plusDays(2))
                .build();
        actionRepository.save(assignmentAction);

        // 2. Google Recruiting
        Email placement = Email.builder()
                .user(user)
                .gmailMessageId("msg-2")
                .gmailThreadId("thread-2")
                .senderName("Google Careers Recruiting")
                .senderEmail("careers@google.com")
                .subject("Shortlisted for Google Software Engineer Internship Interview")
                .bodySnippet("Congratulations! You have been shortlisted for the Software Engineer position. Please reply with your availability for a technical interview.")
                .bodyFull("Congratulations! You have been shortlisted for the Software Engineer position. Please reply with your availability for a technical interview this week. We are excited to chat with you.")
                .receivedAt(now.minusHours(2))
                .isRead(false)
                .hasAttachments(false)
                .category(Email.EmailCategory.PLACEMENT)
                .priority(Email.Priority.HIGH)
                .aiSummary("Shortlisted for Google SWE Interview. Please reply with your availability for a technical interview this week.")
                .deadlineDetected(now.plusDays(1))
                .build();
        placement = emailRepository.save(placement);

        EmailAction placementAction = EmailAction.builder()
                .email(placement)
                .userId(user.getId())
                .actionType(EmailAction.ActionType.REPLY)
                .actionDescription("Reply with interview availability to Google Careers Recruiting")
                .deadline(now.plusDays(1))
                .build();
        actionRepository.save(placementAction);

        // 3. Hackathon
        Email hackathon = Email.builder()
                .user(user)
                .gmailMessageId("msg-3")
                .gmailThreadId("thread-3")
                .senderName("Nexora Dev Community")
                .senderEmail("dev@nexora.io")
                .subject("Nexora Hackathon 2026 Registration Open")
                .bodySnippet("Register for the upcoming Nexora Hackathon 2026. Showcase your AI email assistant project and win up to $5,000 in prizes.")
                .bodyFull("Register for the upcoming Nexora Hackathon 2026. Showcase your AI email assistant project and win up to $5,000 in prizes. Teams can be up to 4 members. Registration closes in 5 days.")
                .receivedAt(now.minusDays(1))
                .isRead(true)
                .hasAttachments(false)
                .category(Email.EmailCategory.HACKATHON)
                .priority(Email.Priority.HIGH)
                .aiSummary("Registration is open for Nexora Hackathon 2026 with a $5,000 prize pool. Showcase your AI email assistant.")
                .deadlineDetected(now.plusDays(5))
                .build();
        hackathon = emailRepository.save(hackathon);

        EmailAction hackathonAction = EmailAction.builder()
                .email(hackathon)
                .userId(user.getId())
                .actionType(EmailAction.ActionType.REGISTER)
                .actionDescription("Register for Nexora Hackathon 2026")
                .deadline(now.plusDays(5))
                .build();
        actionRepository.save(hackathonAction);

        // 4. Meeting
        Email meeting = Email.builder()
                .user(user)
                .gmailMessageId("msg-4")
                .gmailThreadId("thread-4")
                .senderName("Sarah Jenkins")
                .senderEmail("sarah.jenkins@team.com")
                .subject("Project Check-in / Sync Meeting")
                .bodySnippet("Let's sync up today at 3 PM to review the frontend styling, dashboard layout, and integration progress.")
                .bodyFull("Hi Austin, Let's sync up today at 3 PM to review the frontend styling, dashboard layout, and integration progress. The link is meet.google.com/abc-defg-hij.")
                .receivedAt(now.minusHours(1))
                .isRead(false)
                .hasAttachments(false)
                .category(Email.EmailCategory.MEETING)
                .priority(Email.Priority.MEDIUM)
                .aiSummary("Project check-in sync meeting scheduled for today at 3 PM to review styling, dashboard, and integration.")
                .deadlineDetected(now.plusHours(3))
                .build();
        meeting = emailRepository.save(meeting);

        EmailAction meetingAction = EmailAction.builder()
                .email(meeting)
                .userId(user.getId())
                .actionType(EmailAction.ActionType.ATTEND)
                .actionDescription("Attend Project Check-in Sync Meeting")
                .deadline(now.plusHours(3))
                .build();
        actionRepository.save(meetingAction);

        // 5. Announcement
        Email announcement = Email.builder()
                .user(user)
                .gmailMessageId("msg-5")
                .gmailThreadId("thread-5")
                .senderName("Placement Officer")
                .senderEmail("placement@university.edu")
                .subject("Placement Drive Briefing Session Announcement")
                .bodySnippet("There will be a mandatory briefing session for all final-year students regarding upcoming campus placements. Read the guidelines.")
                .bodyFull("Dear Students, There will be a mandatory briefing session for all final-year students regarding upcoming campus placements. Read the guidelines document before attending.")
                .receivedAt(now.minusDays(2))
                .isRead(true)
                .hasAttachments(true)
                .category(Email.EmailCategory.ANNOUNCEMENT)
                .priority(Email.Priority.MEDIUM)
                .aiSummary("Mandatory placement drive briefing session for final-year students announced. Read guidelines before attending.")
                .build();
        announcement = emailRepository.save(announcement);

        EmailAction announcementAction = EmailAction.builder()
                .email(announcement)
                .userId(user.getId())
                .actionType(EmailAction.ActionType.REVIEW)
                .actionDescription("Review placement guidelines document")
                .build();
        actionRepository.save(announcementAction);
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private JsonNode exchangeCodeForTokens(String code, String dynamicRedirectUri) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", code);
        body.add("client_id", googleClientId);
        body.add("client_secret", googleClientSecret);
        
        String effectiveRedirectUri = redirectUri;
        if (effectiveRedirectUri != null && effectiveRedirectUri.contains("localhost") 
                && dynamicRedirectUri != null && !dynamicRedirectUri.contains("localhost")) {
            effectiveRedirectUri = dynamicRedirectUri;
        } else if (effectiveRedirectUri == null || effectiveRedirectUri.isEmpty()) {
            effectiveRedirectUri = dynamicRedirectUri;
        }
        body.add("redirect_uri", effectiveRedirectUri);
        body.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(TOKEN_ENDPOINT, request, String.class);
            return objectMapper.readTree(response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            log.error("Failed to exchange code for tokens. Status: {}, Response: {}", e.getStatusCode(), e.getResponseBodyAsString());
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

    @jakarta.annotation.PostConstruct
    public void init() {
        migrateTokenEncryption();
    }

    @org.springframework.transaction.annotation.Transactional
    public void migrateTokenEncryption() {
        log.info("Checking token encryption compatibility...");
        List<User> users = userRepository.findAll();
        int migratedCount = 0;
        for (User user : users) {
            if (user.getGmailAccessToken() != null) {
                try {
                    // Try to decrypt with new random IV method.
                    // If it succeeded, token is compatible.
                    tokenEncryptor.decrypt(user.getGmailAccessToken());
                } catch (Exception e) {
                    // Encryption mismatch (old static IV token) -> clear to force user re-auth
                    user.setGmailAccessToken(null);
                    user.setGmailRefreshToken(null);
                    user.setTokenExpiry(null);
                    userRepository.save(user);
                    migratedCount++;
                }
            }
        }
        if (migratedCount > 0) {
            log.info("Token migration complete — cleared {} incompatible tokens. Users must reconnect Gmail.", migratedCount);
        } else {
            log.info("Token migration check complete — all tokens compatible.");
        }
    }
}
