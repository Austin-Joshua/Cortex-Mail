package com.nexora.dto.request;

public class DraftRequest {
    private String to;
    private String cc;
    private String bcc;
    private String subject;
    private String body;
    private String htmlBody;
    private Long scheduledSendTime;
    private String draftStatus;

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }
    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }
    public String getBcc() { return bcc; }
    public void setBcc(String bcc) { this.bcc = bcc; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public String getHtmlBody() { return htmlBody; }
    public void setHtmlBody(String htmlBody) { this.htmlBody = htmlBody; }
    public Long getScheduledSendTime() { return scheduledSendTime; }
    public void setScheduledSendTime(Long scheduledSendTime) { this.scheduledSendTime = scheduledSendTime; }
    public String getDraftStatus() { return draftStatus; }
    public void setDraftStatus(String draftStatus) { this.draftStatus = draftStatus; }
}
