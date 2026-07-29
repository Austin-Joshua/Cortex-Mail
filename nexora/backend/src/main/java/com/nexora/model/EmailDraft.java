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

    // Owner back-reference. Serializing it repeated the caller's own
    // record — googleId, timestamps and all — inside every draft or
    // template in the list. The row already belongs to the requester.
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // `to` is a reserved word in SQL — leaving the column name to default from
    // the field produced `to TEXT` and the CREATE TABLE failed outright, so
    // email_drafts was never created. The field name is unchanged, so the JSON
    // the clients send and receive is unaffected.
    @Column(name = "to_recipients", columnDefinition = "TEXT")
    private String to;

    @Column(columnDefinition = "TEXT")
    private String cc;

    @Column(columnDefinition = "TEXT")
    private String bcc;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Lob
    @Column
    private String body;

    @Lob
    @Column
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
