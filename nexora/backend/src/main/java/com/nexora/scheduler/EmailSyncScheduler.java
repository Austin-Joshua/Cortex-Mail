package com.nexora.scheduler;

import com.nexora.model.User;
import com.nexora.repository.BrainConversationRepository;
import com.nexora.repository.UserRepository;
import com.nexora.service.GmailSyncService;
import com.nexora.service.NotificationService;
import com.nexora.service.PostSyncProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Background sync. In-memory {@link GmailSyncService} locks are single-JVM —
 * run one backend instance (or accept duplicate-skip behavior across dynos).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSyncScheduler {

    private static final int MAX_USERS_PER_TICK = 5;

    private final NotificationService notificationService;
    private final BrainConversationRepository brainConversationRepository;
    private final UserRepository userRepository;
    private final GmailSyncService gmailSyncService;
    private final PostSyncProcessingService postSyncProcessingService;

    /**
     * Sync inbox every 5 minutes for stale users — kick async so the scheduler
     * thread is not blocked on Gmail I/O for minutes.
     */
    @Scheduled(fixedDelay = 300_000)
    public void syncAllUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<User> users = userRepository.findAllByLastSyncedAtBeforeOrLastSyncedAtIsNull(threshold);

        int started = 0;
        for (User user : users) {
            if (started >= MAX_USERS_PER_TICK) {
                log.info("Deferring remaining {} users to next scheduler tick", users.size() - started);
                break;
            }
            Long userId = user.getId();
            if (userId == null || gmailSyncService.hasActiveSync(userId)) {
                continue;
            }
            try {
                postSyncProcessingService.syncAndProcess(userId);
                started++;
            } catch (Exception e) {
                log.error("Scheduler sync kick failed for user {}: {}", userId, e.getMessage());
            }
        }
        if (started > 0) {
            log.info("Scheduler kicked background sync for {} users", started);
        }
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void dailyNotifications() {
        List<User> allUsers = userRepository.findAll(PageRequest.of(0, 500)).getContent();
        log.info("Generating daily notifications for {} users", allUsers.size());
        for (User user : allUsers) {
            try {
                notificationService.generateDailyNotifications(user.getId());
            } catch (Exception e) {
                log.error("Daily notification failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldConversations() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        log.info("Cleaning up BrainConversation records older than {}", cutoff);
        try {
            brainConversationRepository.deleteByCreatedAtBefore(cutoff);
            log.info("Old conversation cleanup complete");
        } catch (Exception e) {
            log.error("Conversation cleanup failed: {}", e.getMessage());
        }
    }
}
