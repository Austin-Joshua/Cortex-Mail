package com.nexora.dto.response;

import java.util.Map;

public class GmailSyncResponse {
    private String message;
    private int newCount;
    private int updatedCount;
    private int inboxMessagesProcessed;
    private Map<String, GmailLabelCountResponse> labelCounts;

    public GmailSyncResponse() {}

    public GmailSyncResponse(String message, int newCount, int updatedCount,
                             int inboxMessagesProcessed,
                             Map<String, GmailLabelCountResponse> labelCounts) {
        this.message = message;
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.inboxMessagesProcessed = inboxMessagesProcessed;
        this.labelCounts = labelCounts;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getNewCount() { return newCount; }
    public void setNewCount(int newCount) { this.newCount = newCount; }

    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }

    public int getInboxMessagesProcessed() { return inboxMessagesProcessed; }
    public void setInboxMessagesProcessed(int inboxMessagesProcessed) {
        this.inboxMessagesProcessed = inboxMessagesProcessed;
    }

    public Map<String, GmailLabelCountResponse> getLabelCounts() { return labelCounts; }
    public void setLabelCounts(Map<String, GmailLabelCountResponse> labelCounts) {
        this.labelCounts = labelCounts;
    }
}
