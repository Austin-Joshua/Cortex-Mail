package com.nexora.dto.response;

import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import com.nexora.model.Email.Reaction;
import com.nexora.model.EmailAction;

import java.time.LocalDateTime;
import java.util.List;

public class EmailResponse {
    private Long id;
    private String gmailMessageId;
    private String gmailThreadId;
    private String senderName;
    private String senderEmail;
    private String subject;
    private String bodySnippet;
    private String bodyFull;
    private String bodyHtml;
    private LocalDateTime receivedAt;
    private Boolean isRead;
    private Boolean hasAttachments;
    private String gmailLabelIds;
    private String recipientTo;
    private String recipientCc;
    private Boolean isStarred;
    private Boolean isImportant;
    private Boolean inInbox;
    private Boolean isDraft;
    private Boolean isArchived;
    private Boolean isTrash;
    private Boolean isSpam;
    private Long sizeEstimate;
    private EmailCategory category;
    private Priority priority;
    private Reaction reaction;
    private String aiSummary;
    private String aiActionItems;
    private LocalDateTime deadlineDetected;
    private Boolean isDeadlineAddedToCalendar;
    private List<ActionItemDto> actions;
    private List<AttachmentDto> attachments;
    private LocalDateTime createdAt;

    public EmailResponse() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGmailMessageId() {
        return gmailMessageId;
    }

    public void setGmailMessageId(String gmailMessageId) {
        this.gmailMessageId = gmailMessageId;
    }

    public String getGmailThreadId() {
        return gmailThreadId;
    }

    public void setGmailThreadId(String gmailThreadId) {
        this.gmailThreadId = gmailThreadId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodySnippet() {
        return bodySnippet;
    }

    public void setBodySnippet(String bodySnippet) {
        this.bodySnippet = bodySnippet;
    }

    public String getBodyFull() {
        return bodyFull;
    }

    public void setBodyFull(String bodyFull) {
        this.bodyFull = bodyFull;
    }

    public String getBodyHtml() {
        return bodyHtml;
    }

    public void setBodyHtml(String bodyHtml) {
        this.bodyHtml = bodyHtml;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public Boolean getHasAttachments() {
        return hasAttachments;
    }

    public void setHasAttachments(Boolean hasAttachments) {
        this.hasAttachments = hasAttachments;
    }

    public String getGmailLabelIds() { return gmailLabelIds; }
    public void setGmailLabelIds(String gmailLabelIds) { this.gmailLabelIds = gmailLabelIds; }

    public String getRecipientTo() { return recipientTo; }
    public void setRecipientTo(String recipientTo) { this.recipientTo = recipientTo; }

    public String getRecipientCc() { return recipientCc; }
    public void setRecipientCc(String recipientCc) { this.recipientCc = recipientCc; }

    public Boolean getIsStarred() { return isStarred; }
    public void setIsStarred(Boolean isStarred) { this.isStarred = isStarred; }

    public Boolean getIsImportant() { return isImportant; }
    public void setIsImportant(Boolean isImportant) { this.isImportant = isImportant; }

    public Boolean getInInbox() { return inInbox; }
    public void setInInbox(Boolean inInbox) { this.inInbox = inInbox; }

    public Boolean getIsDraft() { return isDraft; }
    public void setIsDraft(Boolean isDraft) { this.isDraft = isDraft; }

    public Boolean getIsArchived() { return isArchived; }
    public void setIsArchived(Boolean isArchived) { this.isArchived = isArchived; }

    public Boolean getIsTrash() { return isTrash; }
    public void setIsTrash(Boolean isTrash) { this.isTrash = isTrash; }

    public Boolean getIsSpam() { return isSpam; }
    public void setIsSpam(Boolean isSpam) { this.isSpam = isSpam; }

    public Long getSizeEstimate() { return sizeEstimate; }
    public void setSizeEstimate(Long sizeEstimate) { this.sizeEstimate = sizeEstimate; }

    public EmailCategory getCategory() {
        return category;
    }

    public void setCategory(EmailCategory category) {
        this.category = category;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Reaction getReaction() {
        return reaction;
    }

    public void setReaction(Reaction reaction) {
        this.reaction = reaction;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getAiActionItems() {
        return aiActionItems;
    }

    public void setAiActionItems(String aiActionItems) {
        this.aiActionItems = aiActionItems;
    }

    public LocalDateTime getDeadlineDetected() {
        return deadlineDetected;
    }

    public void setDeadlineDetected(LocalDateTime deadlineDetected) {
        this.deadlineDetected = deadlineDetected;
    }

    public Boolean getIsDeadlineAddedToCalendar() {
        return isDeadlineAddedToCalendar;
    }

    public void setIsDeadlineAddedToCalendar(Boolean isDeadlineAddedToCalendar) {
        this.isDeadlineAddedToCalendar = isDeadlineAddedToCalendar;
    }

    public List<ActionItemDto> getActions() {
        return actions;
    }

    public void setActions(List<ActionItemDto> actions) {
        this.actions = actions;
    }

    public List<AttachmentDto> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<AttachmentDto> attachments) {
        this.attachments = attachments;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static class AttachmentDto {
        private Long id;
        private String filename;
        private String mimeType;
        private Long sizeBytes;
        private Boolean isInline;

        public AttachmentDto() {}

        public AttachmentDto(Long id, String filename, String mimeType, Long sizeBytes, Boolean isInline) {
            this.id = id;
            this.filename = filename;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
            this.isInline = isInline;
        }

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        public Long getSizeBytes() { return sizeBytes; }
        public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
        public Boolean getIsInline() { return isInline; }
        public void setIsInline(Boolean isInline) { this.isInline = isInline; }

        public static AttachmentDtoBuilder builder() {
            return new AttachmentDtoBuilder();
        }

        public static class AttachmentDtoBuilder {
            private Long id;
            private String filename;
            private String mimeType;
            private Long sizeBytes;
            private Boolean isInline;

            public AttachmentDtoBuilder id(Long id) { this.id = id; return this; }
            public AttachmentDtoBuilder filename(String filename) { this.filename = filename; return this; }
            public AttachmentDtoBuilder mimeType(String mimeType) { this.mimeType = mimeType; return this; }
            public AttachmentDtoBuilder sizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; return this; }
            public AttachmentDtoBuilder isInline(Boolean isInline) { this.isInline = isInline; return this; }
            public AttachmentDto build() {
                return new AttachmentDto(id, filename, mimeType, sizeBytes, isInline);
            }
        }
    }

    public static class ActionItemDto {
        private Long id;
        private EmailAction.ActionType actionType;
        private String actionDescription;
        private LocalDateTime deadline;
        private Boolean isCompleted;

        public ActionItemDto() {}

        public ActionItemDto(Long id, EmailAction.ActionType actionType, String actionDescription,
                             LocalDateTime deadline, Boolean isCompleted) {
            this.id = id;
            this.actionType = actionType;
            this.actionDescription = actionDescription;
            this.deadline = deadline;
            this.isCompleted = isCompleted;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public EmailAction.ActionType getActionType() {
            return actionType;
        }

        public void setActionType(EmailAction.ActionType actionType) {
            this.actionType = actionType;
        }

        public String getActionDescription() {
            return actionDescription;
        }

        public void setActionDescription(String actionDescription) {
            this.actionDescription = actionDescription;
        }

        public LocalDateTime getDeadline() {
            return deadline;
        }

        public void setDeadline(LocalDateTime deadline) {
            this.deadline = deadline;
        }

        public Boolean getIsCompleted() {
            return isCompleted;
        }

        public void setIsCompleted(Boolean isCompleted) {
            this.isCompleted = isCompleted;
        }

        public static ActionItemDtoBuilder builder() {
            return new ActionItemDtoBuilder();
        }

        public static class ActionItemDtoBuilder {
            private Long id;
            private EmailAction.ActionType actionType;
            private String actionDescription;
            private LocalDateTime deadline;
            private Boolean isCompleted;

            ActionItemDtoBuilder() {}

            public ActionItemDtoBuilder id(Long id) {
                this.id = id;
                return this;
            }

            public ActionItemDtoBuilder actionType(EmailAction.ActionType actionType) {
                this.actionType = actionType;
                return this;
            }

            public ActionItemDtoBuilder actionDescription(String actionDescription) {
                this.actionDescription = actionDescription;
                return this;
            }

            public ActionItemDtoBuilder deadline(LocalDateTime deadline) {
                this.deadline = deadline;
                return this;
            }

            public ActionItemDtoBuilder isCompleted(Boolean isCompleted) {
                this.isCompleted = isCompleted;
                return this;
            }

            public ActionItemDto build() {
                return new ActionItemDto(this.id, this.actionType, this.actionDescription, this.deadline, this.isCompleted);
            }
        }
    }

    public static EmailResponseBuilder builder() {
        return new EmailResponseBuilder();
    }

    public static class EmailResponseBuilder {
        private Long id;
        private String gmailMessageId;
        private String gmailThreadId;
        private String senderName;
        private String senderEmail;
        private String subject;
        private String bodySnippet;
        private String bodyFull;
        private String bodyHtml;
        private LocalDateTime receivedAt;
        private Boolean isRead;
        private Boolean hasAttachments;
        private String gmailLabelIds;
        private String recipientTo;
        private String recipientCc;
        private Boolean isStarred;
        private Boolean isImportant;
        private Boolean inInbox;
        private Boolean isDraft;
        private Boolean isArchived;
        private Boolean isTrash;
        private Boolean isSpam;
        private Long sizeEstimate;
        private EmailCategory category;
        private Priority priority;
        private Reaction reaction;
        private String aiSummary;
        private String aiActionItems;
        private LocalDateTime deadlineDetected;
        private Boolean isDeadlineAddedToCalendar;
        private List<ActionItemDto> actions;
        private List<AttachmentDto> attachments;
        private LocalDateTime createdAt;

        EmailResponseBuilder() {}

        public EmailResponseBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public EmailResponseBuilder gmailMessageId(String gmailMessageId) {
            this.gmailMessageId = gmailMessageId;
            return this;
        }

        public EmailResponseBuilder gmailThreadId(String gmailThreadId) {
            this.gmailThreadId = gmailThreadId;
            return this;
        }

        public EmailResponseBuilder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public EmailResponseBuilder senderEmail(String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        public EmailResponseBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public EmailResponseBuilder bodySnippet(String bodySnippet) {
            this.bodySnippet = bodySnippet;
            return this;
        }

        public EmailResponseBuilder bodyFull(String bodyFull) {
            this.bodyFull = bodyFull;
            return this;
        }

        public EmailResponseBuilder bodyHtml(String bodyHtml) {
            this.bodyHtml = bodyHtml;
            return this;
        }

        public EmailResponseBuilder receivedAt(LocalDateTime receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public EmailResponseBuilder isRead(Boolean isRead) {
            this.isRead = isRead;
            return this;
        }

        public EmailResponseBuilder hasAttachments(Boolean hasAttachments) {
            this.hasAttachments = hasAttachments;
            return this;
        }

        public EmailResponseBuilder gmailLabelIds(String gmailLabelIds) {
            this.gmailLabelIds = gmailLabelIds;
            return this;
        }

        public EmailResponseBuilder recipientTo(String recipientTo) {
            this.recipientTo = recipientTo;
            return this;
        }

        public EmailResponseBuilder recipientCc(String recipientCc) {
            this.recipientCc = recipientCc;
            return this;
        }

        public EmailResponseBuilder isStarred(Boolean isStarred) {
            this.isStarred = isStarred;
            return this;
        }

        public EmailResponseBuilder isImportant(Boolean isImportant) {
            this.isImportant = isImportant;
            return this;
        }

        public EmailResponseBuilder inInbox(Boolean inInbox) {
            this.inInbox = inInbox;
            return this;
        }

        public EmailResponseBuilder isDraft(Boolean isDraft) {
            this.isDraft = isDraft;
            return this;
        }

        public EmailResponseBuilder isArchived(Boolean isArchived) {
            this.isArchived = isArchived;
            return this;
        }

        public EmailResponseBuilder isTrash(Boolean isTrash) {
            this.isTrash = isTrash;
            return this;
        }

        public EmailResponseBuilder isSpam(Boolean isSpam) {
            this.isSpam = isSpam;
            return this;
        }

        public EmailResponseBuilder sizeEstimate(Long sizeEstimate) {
            this.sizeEstimate = sizeEstimate;
            return this;
        }

        public EmailResponseBuilder category(EmailCategory category) {
            this.category = category;
            return this;
        }

        public EmailResponseBuilder priority(Priority priority) {
            this.priority = priority;
            return this;
        }

        public EmailResponseBuilder reaction(Reaction reaction) {
            this.reaction = reaction;
            return this;
        }

        public EmailResponseBuilder aiSummary(String aiSummary) {
            this.aiSummary = aiSummary;
            return this;
        }

        public EmailResponseBuilder aiActionItems(String aiActionItems) {
            this.aiActionItems = aiActionItems;
            return this;
        }

        public EmailResponseBuilder deadlineDetected(LocalDateTime deadlineDetected) {
            this.deadlineDetected = deadlineDetected;
            return this;
        }

        public EmailResponseBuilder isDeadlineAddedToCalendar(Boolean isDeadlineAddedToCalendar) {
            this.isDeadlineAddedToCalendar = isDeadlineAddedToCalendar;
            return this;
        }

        public EmailResponseBuilder actions(List<ActionItemDto> actions) {
            this.actions = actions;
            return this;
        }

        public EmailResponseBuilder attachments(List<AttachmentDto> attachments) {
            this.attachments = attachments;
            return this;
        }

        public EmailResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public EmailResponse build() {
            EmailResponse r = new EmailResponse();
            r.id = this.id;
            r.gmailMessageId = this.gmailMessageId;
            r.gmailThreadId = this.gmailThreadId;
            r.senderName = this.senderName;
            r.senderEmail = this.senderEmail;
            r.subject = this.subject;
            r.bodySnippet = this.bodySnippet;
            r.bodyFull = this.bodyFull;
            r.bodyHtml = this.bodyHtml;
            r.receivedAt = this.receivedAt;
            r.isRead = this.isRead;
            r.hasAttachments = this.hasAttachments;
            r.gmailLabelIds = this.gmailLabelIds;
            r.recipientTo = this.recipientTo;
            r.recipientCc = this.recipientCc;
            r.isStarred = this.isStarred;
            r.isImportant = this.isImportant;
            r.inInbox = this.inInbox;
            r.isDraft = this.isDraft;
            r.isArchived = this.isArchived;
            r.isTrash = this.isTrash;
            r.isSpam = this.isSpam;
            r.sizeEstimate = this.sizeEstimate;
            r.category = this.category;
            r.priority = this.priority;
            r.reaction = this.reaction;
            r.aiSummary = this.aiSummary;
            r.aiActionItems = this.aiActionItems;
            r.deadlineDetected = this.deadlineDetected;
            r.isDeadlineAddedToCalendar = this.isDeadlineAddedToCalendar;
            r.actions = this.actions;
            r.attachments = this.attachments;
            r.createdAt = this.createdAt;
            return r;
        }
    }
}
