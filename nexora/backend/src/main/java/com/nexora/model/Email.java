package com.nexora.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "emails",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_emails_user_gmail_message", columnNames = {"user_id", "gmail_message_id"})
    },
    indexes = {
        @Index(name = "idx_user_category", columnList = "user_id, category"),
        @Index(name = "idx_user_priority", columnList = "user_id, priority"),
        @Index(name = "idx_user_received", columnList = "user_id, received_at"),
        @Index(name = "idx_emails_user_thread", columnList = "user_id, gmail_thread_id"),
        @Index(name = "idx_emails_user_inbox_received", columnList = "user_id, in_inbox, received_at"),
        @Index(name = "idx_emails_user_unread", columnList = "user_id, is_read, in_inbox")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Email {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(name = "gmail_message_id", nullable = false)
    private String gmailMessageId;

    @Column(name = "gmail_thread_id")
    private String gmailThreadId;

    @Column(name = "sender_name")
    private String senderName;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(columnDefinition = "TEXT")
    private String subject;

    @Column(name = "body_snippet", columnDefinition = "TEXT")
    private String bodySnippet;

    /** Normalized plain text for AI / search (Gmail-derived, not invented). */
    @JsonIgnore
    @Column(name = "body_full", columnDefinition = "TEXT")
    private String bodyFull;

    /** Sanitized HTML for UI rendering (sanitized server-side at MIME extract time). */
    @JsonIgnore
    @Column(name = "body_html", columnDefinition = "TEXT")
    private String bodyHtml;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "has_attachments")
    @Builder.Default
    private Boolean hasAttachments = false;

    /** Gmail label IDs as JSON array, e.g. ["INBOX","UNREAD","CATEGORY_PERSONAL"] */
    @Column(name = "gmail_label_ids", columnDefinition = "TEXT")
    private String gmailLabelIds;

    @Column(name = "recipient_to", columnDefinition = "TEXT")
    private String recipientTo;

    @Column(name = "recipient_cc", columnDefinition = "TEXT")
    private String recipientCc;

    @Column(name = "recipient_bcc", columnDefinition = "TEXT")
    private String recipientBcc;

    @Column(name = "reply_to")
    private String replyTo;

    @Column(name = "size_estimate")
    private Long sizeEstimate;

    @Column(name = "is_starred")
    @Builder.Default
    private Boolean isStarred = false;

    @Column(name = "is_important")
    @Builder.Default
    private Boolean isImportant = false;

    /** True when the message currently has the INBOX label in Gmail. */
    @Column(name = "in_inbox")
    @Builder.Default
    private Boolean inInbox = true;

    /** True when the message has the Gmail DRAFT label. */
    @Column(name = "is_draft")
    @Builder.Default
    private Boolean isDraft = false;

    /**
     * True when mail is archived in Gmail: not in INBOX, not DRAFT, not TRASH, not SPAM.
     */
    @Column(name = "is_archived")
    @Builder.Default
    private Boolean isArchived = false;

    @Column(name = "is_trash")
    @Builder.Default
    private Boolean isTrash = false;

    @Column(name = "is_spam")
    @Builder.Default
    private Boolean isSpam = false;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EmailCategory category = EmailCategory.UNCATEGORIZED;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Reaction reaction = Reaction.NONE;

    @Column(name = "ai_summary", columnDefinition = "TEXT")
    private String aiSummary;

    @Column(name = "ai_action_items", columnDefinition = "TEXT")
    private String aiActionItems;

    @Column(name = "deadline_detected")
    private LocalDateTime deadlineDetected;

    @Column(name = "is_deadline_added_to_calendar")
    @Builder.Default
    private Boolean isDeadlineAddedToCalendar = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmailAction> actions = new ArrayList<>();

    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<EmailAttachment> attachments = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum EmailCategory {
        ASSIGNMENT, ATTENDANCE, HACKATHON, PLACEMENT, INTERNSHIP,
        MEETING, ANNOUNCEMENT, RESEARCH, FINANCE, PERSONAL,
        PROMOTIONAL, SPAM, UNCATEGORIZED
    }

    public enum Priority {
        HIGH, MEDIUM, LOW
    }

    public enum Reaction {
        NONE, DONE, IMPORTANT, LATER, IGNORE, SNOOZED
    }
}
