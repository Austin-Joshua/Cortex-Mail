package com.nexora.scheduler;

import com.nexora.model.User;
import com.nexora.repository.BrainConversationRepository;
import com.nexora.repository.UserRepository;
import com.nexora.service.GmailSyncService;
import com.nexora.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSyncScheduler {

    private final GmailSyncService gmailSyncService;
    private final NotificationService notificationService;
    private final BrainConversationRepository brainConversationRepository;
    private final UserRepository userRepository;

    /**
     * Sync inbox every 5 minutes for all users whose last sync was > 5 min ago.
     * Uses unified syncInbox which prefers incremental (history.list) when a
     * historyId is stored, and falls back to full sync on first sync / recovery.
     */
    @Scheduled(fixedDelay = 300_000)
    public void syncAllUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<User> users = userRepository.findAllByLastSyncedAtBeforeOrLastSyncedAtIsNull(threshold);

        log.info("Email sync triggered for {} users (incremental preferred)", users.size());

        for (User user : users) {
            try {
                gmailSyncService.syncInbox(user.getId());
            } catch (Exception e) {
                log.error("Sync failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /**
     * Daily notifications at 8:00 AM every day.
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void dailyNotifications() {
        List<User> allUsers = userRepository.findAll();
        log.info("Generating daily notifications for {} users", allUsers.size());
        for (User user : allUsers) {
            try {
                notificationService.generateDailyNotifications(user.getId());
            } catch (Exception e) {
                log.error("Daily notification failed for user {}: {}", user.getId(), e.getMessage());
            }
        }
    }

    /**
     * Daily cleanup at 2:00 AM — delete BrainConversation records older than 30 days.
     */
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

