package com.nexora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "google_id", unique = true, nullable = false)
    private String googleId;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(name = "profile_picture_url", columnDefinition = "TEXT")
    private String profilePictureUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_role")
    @Builder.Default
    private UserRole userRole = UserRole.STUDENT;

    @JsonIgnore
    @Column(name = "gmail_access_token", columnDefinition = "TEXT")
    private String gmailAccessToken;

    @JsonIgnore
    @Column(name = "gmail_refresh_token", columnDefinition = "TEXT")
    private String gmailRefreshToken;

    @Column(name = "token_expiry")
    private LocalDateTime tokenExpiry;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    /** Cached Gmail label counts (JSON) from the last sync — matches the real Gmail account. */
    @JsonIgnore
    @Column(name = "gmail_label_counts", columnDefinition = "TEXT")
    private String gmailLabelCounts;

    @JsonIgnore
    @Column(name = "gmail_history_id")
    private String gmailHistoryId;

    @Column(name = "calendar_sync_enabled")
    @Builder.Default
    private Boolean calendarSyncEnabled = true;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Email> emails;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum UserRole {
        STUDENT, PROFESSOR, IT_EMPLOYEE, HR_PROFESSIONAL, MANAGER, FREELANCER
    }
}
