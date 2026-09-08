package com.nexora.dto.response;

public class EmailActionResponse {
    private Long id;
    private Long emailId;
    private String emailSubject;
    private String actionType;
    private String actionDescription;
    private String deadline;
    private Boolean isCompleted;
    private String snoozedUntil;

    public EmailActionResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmailId() { return emailId; }
    public void setEmailId(Long emailId) { this.emailId = emailId; }

    public String getEmailSubject() { return emailSubject; }
    public void setEmailSubject(String emailSubject) { this.emailSubject = emailSubject; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean isCompleted) { this.isCompleted = isCompleted; }

    public String getSnoozedUntil() { return snoozedUntil; }
    public void setSnoozedUntil(String snoozedUntil) { this.snoozedUntil = snoozedUntil; }
}
