package com.nexora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_attachments", indexes = {
        @Index(name = "idx_email_attachments_email", columnList = "email_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_id", nullable = false)
    @JsonIgnore
    private Email email;

    @Column(name = "gmail_attachment_id", length = 512)
    private String gmailAttachmentId;

    @Column(length = 1024)
    private String filename;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "content_id", length = 512)
    private String contentId;

    @Column(name = "is_inline")
    @Builder.Default
    private Boolean isInline = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
