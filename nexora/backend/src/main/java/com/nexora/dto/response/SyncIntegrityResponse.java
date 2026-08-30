package com.nexora.dto.response;

import java.util.List;
import java.util.Map;

/**
 * Side-by-side check: Gmail Labels API vs what Cortex has stored/classified.
 */
public class SyncIntegrityResponse {
    private boolean connected;
    private String lastSyncedAt;
    private Map<String, Long> gmailCounts;
    private Map<String, Long> localCounts;
    private Map<String, Long> categoryGroups;
    private long unclassifiedInbox;
    private boolean inboxAligned;
    private boolean draftsAligned;
    private boolean secondaryComplete;
    private boolean syncInProgress;
    private List<String> notes;
    private List<Map<String, Object>> sampleInbox;

    public SyncIntegrityResponse() {}

    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) { this.connected = connected; }

    public String getLastSyncedAt() { return lastSyncedAt; }
    public void setLastSyncedAt(String lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }

    public Map<String, Long> getGmailCounts() { return gmailCounts; }
    public void setGmailCounts(Map<String, Long> gmailCounts) { this.gmailCounts = gmailCounts; }

    public Map<String, Long> getLocalCounts() { return localCounts; }
    public void setLocalCounts(Map<String, Long> localCounts) { this.localCounts = localCounts; }

    public Map<String, Long> getCategoryGroups() { return categoryGroups; }
    public void setCategoryGroups(Map<String, Long> categoryGroups) { this.categoryGroups = categoryGroups; }

    public long getUnclassifiedInbox() { return unclassifiedInbox; }
    public void setUnclassifiedInbox(long unclassifiedInbox) { this.unclassifiedInbox = unclassifiedInbox; }

    public boolean isInboxAligned() { return inboxAligned; }
    public void setInboxAligned(boolean inboxAligned) { this.inboxAligned = inboxAligned; }

    public boolean isDraftsAligned() { return draftsAligned; }
    public void setDraftsAligned(boolean draftsAligned) { this.draftsAligned = draftsAligned; }

    /** True once FAST_FIRST background pass (or a full sync) has stored a Gmail historyId. */
    public boolean isSecondaryComplete() { return secondaryComplete; }
    public void setSecondaryComplete(boolean secondaryComplete) { this.secondaryComplete = secondaryComplete; }

    public boolean isSyncInProgress() { return syncInProgress; }
    public void setSyncInProgress(boolean syncInProgress) { this.syncInProgress = syncInProgress; }

    public List<String> getNotes() { return notes; }
    public void setNotes(List<String> notes) { this.notes = notes; }

    public List<Map<String, Object>> getSampleInbox() { return sampleInbox; }
    public void setSampleInbox(List<Map<String, Object>> sampleInbox) { this.sampleInbox = sampleInbox; }
}
