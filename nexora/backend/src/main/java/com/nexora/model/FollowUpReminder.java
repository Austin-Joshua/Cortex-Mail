package com.nexora.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "followup_reminders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpReminder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "email_id")
    private Email email;

    private String emailMessageId;

    private String senderEmail;

    private String subject;

    private LocalDateTime reminderTime;

    private String status; // PENDING, NOTIFIED, COMPLETED, SNOOZED

    private LocalDateTime snoozedUntil;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        status = "PENDING";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
