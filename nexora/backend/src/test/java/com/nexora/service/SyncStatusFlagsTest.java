package com.nexora.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyncStatusFlagsTest {

    @Test
    void secondaryCompleteRequiresHistoryId() {
        assertFalse(SyncStatusFlags.secondaryComplete(null));
        assertFalse(SyncStatusFlags.secondaryComplete(""));
        assertFalse(SyncStatusFlags.secondaryComplete("   "));
        assertTrue(SyncStatusFlags.secondaryComplete("12345"));
    }

    @Test
    void enrichingOnlyWhileSyncRunningWithoutHistory() {
        assertTrue(SyncStatusFlags.enrichingInBackground(true, false, true));
        assertFalse(SyncStatusFlags.enrichingInBackground(true, false, false));
        assertFalse(SyncStatusFlags.enrichingInBackground(true, true, true));
        assertFalse(SyncStatusFlags.enrichingInBackground(false, false, true));
    }

    @Test
    void incompleteButIdleWhenSyncStoppedWithoutHistory() {
        assertTrue(SyncStatusFlags.enrichmentIncompleteButIdle(false, false, true));
        assertFalse(SyncStatusFlags.enrichmentIncompleteButIdle(false, true, true));
        assertFalse(SyncStatusFlags.enrichmentIncompleteButIdle(true, false, true));
        assertFalse(SyncStatusFlags.enrichmentIncompleteButIdle(false, false, false));
    }
}
