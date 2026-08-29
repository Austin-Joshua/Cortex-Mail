package com.nexora.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "brain_conversations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrainConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "user_query", columnDefinition = "TEXT", nullable = false)
    private String userQuery;

    @Column(name = "ai_response", columnDefinition = "TEXT", nullable = false)
    private String aiResponse;

    @Column(name = "referenced_email_ids", columnDefinition = "TEXT")
    private String referencedEmailIds;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
