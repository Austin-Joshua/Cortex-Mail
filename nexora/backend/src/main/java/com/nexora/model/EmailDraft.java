package com.nexora.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_drafts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailDraft {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "to_address", columnDefinition = "TEXT")
    private String to;

    @Column(columnDefinition = "TEXT")
    private String cc;

    @Column(columnDefinition = "TEXT")
    private String bcc;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String body;

    @Column(columnDefinition = "TEXT")
    private String htmlBody;

    private Long scheduledSendTime;

    private String draftStatus; // DRAFT, SCHEDULED, SENT

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
