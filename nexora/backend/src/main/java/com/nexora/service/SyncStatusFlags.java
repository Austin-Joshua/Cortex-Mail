package com.nexora.service;

/**
 * Pure helpers for sync-status flags exposed to the UI.
 * Keeps secondary-complete / enriching semantics unit-testable without Spring.
 */
public final class SyncStatusFlags {

    private SyncStatusFlags() {}

    /** Secondary enrichment finished when a Gmail history cursor is stored. */
    public static boolean secondaryComplete(String gmailHistoryId) {
        return gmailHistoryId != null && !gmailHistoryId.isBlank();
    }

    /**
     * True while mail is stored but full enrichment is still running
     * (historyId not yet written and a sync lock is held).
     */
    public static boolean enrichingInBackground(
            boolean hasSyncedMail,
            boolean secondaryComplete,
            boolean syncInProgress) {
        return hasSyncedMail && !secondaryComplete && syncInProgress;
    }

    /** Soft note when enrichment stopped without a history cursor. */
    public static boolean enrichmentIncompleteButIdle(
            boolean secondaryComplete,
            boolean syncInProgress,
            boolean hasLastSyncedAt) {
        return !secondaryComplete && hasLastSyncedAt && !syncInProgress;
    }
}
