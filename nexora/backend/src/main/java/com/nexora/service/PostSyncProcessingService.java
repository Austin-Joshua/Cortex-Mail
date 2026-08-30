package com.nexora.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Runs Gmail sync + post-sync work through Spring's async proxy.
 *
 * Keeping this in a separate bean is intentional: calling an {@code @Async}
 * method from another method on the same bean executes synchronously.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PostSyncProcessingService {

    private final GmailSyncService gmailSyncService;
    private final EmailClassificationService classificationService;

    /**
     * Full sync pipeline off the HTTP thread: Gmail sync, then classify / secondary / refine.
     */
    @Async("syncExecutor")
    public void syncAndProcess(Long userId) {
        try {
            var response = gmailSyncService.syncInbox(userId);
            String mode = response.getSyncMode();
            if (mode != null && !"SKIPPED".equals(mode)) {
                processInline(userId, mode);
            }
        } catch (Exception e) {
            log.error("Background sync failed for user {}: {}", userId, e.getMessage());
        }
    }

    /** Post-sync only (when sync already ran on the caller thread). */
    @Async("syncExecutor")
    public void process(Long userId, String syncMode) {
        processInline(userId, syncMode);
    }

    @Async("taskExecutor")
    public void classifyAndRefine(Long userId, boolean force) {
        try {
            if (force) {
                // Quiet re-analyze: no category wipe — keeps UI stable while groups refresh.
                classificationService.classifyInboxBySourceAndContent(userId);
                classificationService.reclassifyRecentInboxInPlace(userId, 400);
            } else {
                classificationService.classifyInboxBySourceAndContent(userId);
            }
            classificationService.refineInboxWithGemini(userId);
        } catch (Exception e) {
            log.error("Background classify failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void processInline(Long userId, String syncMode) {
        // FAST_FIRST: METADATA → quick local groups for the UI (no category wipe),
        // then FULL body enrichment, then light reclassify of recent mail only.
        classify(userId, "initial");

        if ("FAST_FIRST".equals(syncMode)) {
            try {
                gmailSyncService.syncSecondaryMailboxes(userId);
                log.info("Light reclassify after FULL bodies for user {} (no category wipe)", userId);
                classificationService.classifyInboxBySourceAndContent(userId);
                classificationService.reclassifyRecentInboxInPlace(userId, 150);
            } catch (Exception e) {
                log.error("Secondary mailbox / reclassify failed for user {}: {}", userId, e.getMessage());
                classify(userId, "secondary-fallback");
            }
        }

        try {
            classificationService.refineInboxWithGemini(userId);
        } catch (Exception e) {
            log.error("Gemini refinement failed for user {}: {}", userId, e.getMessage());
        }
    }

    private void classify(Long userId, String phase) {
        try {
            int classified = classificationService.classifyInboxBySourceAndContent(userId);
            log.info("{} post-sync classification for user {} processed {} emails",
                    phase, userId, classified);
        } catch (Exception e) {
            log.error("{} post-sync classification failed for user {}: {}",
                    phase, userId, e.getMessage());
        }
    }
}
