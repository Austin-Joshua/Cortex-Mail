package com.nexora.service;

import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.EmailAction;
import com.nexora.model.Notification;
import com.nexora.model.User;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.NotificationRepository;
import com.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;
    private final UserRepository userRepository;

    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NexoraException("Notification not found", 404));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @org.springframework.transaction.annotation.Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllReadByUserId(userId);
    }

    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    /**
     * Called by scheduler — generate daily digest and deadline notifications.
     */
    @org.springframework.transaction.annotation.Transactional
    public void generateDailyNotifications(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);
        LocalDateTime sinceMidnight = now.toLocalDate().atStartOfDay();

        if (user != null && Boolean.TRUE.equals(user.getDigestEnabled() != null ? user.getDigestEnabled() : true)) {
            Integer digestHour = user.getDigestHour() != null ? user.getDigestHour() : 8;
            if (now.getHour() == digestHour
                    && !notificationRepository.existsByUserIdAndNotificationTypeAndCreatedAtAfter(
                    userId, Notification.NotificationType.DAILY_DIGEST, sinceMidnight)) {
                long unread = emailRepository.countInboxUnreadByUserId(userId);
                long overdue = emailRepository.countOverdueDeadlines(userId, now, now.minusDays(14));
                long pending = actionRepository.countOpenInboxFollowUps(userId, LocalDateTime.now());
                String message = "Digest: " + unread + " unread, " + overdue + " overdue, "
                        + pending + " open follow-ups";
                Notification digest = Notification.builder()
                        .userId(userId)
                        .title("Daily digest")
                        .message(message)
                        .notificationType(Notification.NotificationType.DAILY_DIGEST)
                        .build();
                notificationRepository.save(digest);
            }
        }

        List<EmailAction> urgentActions = actionRepository
                .findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(userId, now, tomorrow);

        for (EmailAction action : urgentActions) {
            if (Boolean.TRUE.equals(action.getIsCompleted())) continue;
            Long emailId = action.getEmail() != null ? action.getEmail().getId() : null;
            String category = action.getEmail() != null && action.getEmail().getCategory() != null
                    ? action.getEmail().getCategory().name() : null;
            if (shouldSkip(user, category)) {
                continue;
            }
            if (emailId != null && notificationRepository.existsByUserIdAndRelatedEmailIdAndNotificationTypeAndCreatedAtAfter(
                    userId, emailId, Notification.NotificationType.DEADLINE, sinceMidnight)) {
                continue;
            }
            createNotification(userId, "Deadline soon",
                    action.getActionDescription() + " — due " + action.getDeadline(),
                    Notification.NotificationType.DEADLINE, emailId, category);
        }

        LocalDateTime since = now.minusHours(24);
        List<Email> highPriorityEmails = emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Email.Priority.HIGH,
                        org.springframework.data.domain.PageRequest.of(0, 5));

        for (Email email : highPriorityEmails) {
            if (email.getReceivedAt() == null || !email.getReceivedAt().isAfter(since)) continue;
            String category = email.getCategory() != null ? email.getCategory().name() : null;
            if (shouldSkip(user, category)) {
                continue;
            }
            if (notificationRepository.existsByUserIdAndRelatedEmailIdAndNotificationTypeAndCreatedAtAfter(
                    userId, email.getId(), Notification.NotificationType.IMPORTANT_EMAIL, sinceMidnight)) {
                continue;
            }
            createNotification(userId, "Important email",
                    "High priority email from " + email.getSenderName() + ": " + email.getSubject(),
                    Notification.NotificationType.IMPORTANT_EMAIL, email.getId(), category);
        }
    }

    public void createNotification(Long userId, String title, String message,
                                    Notification.NotificationType type, Long relatedEmailId) {
        createNotification(userId, title, message, type, relatedEmailId, null);
    }

    public void createNotification(Long userId, String title, String message,
                                    Notification.NotificationType type, Long relatedEmailId,
                                    String category) {
        User user = userRepository.findById(userId).orElse(null);
        if (shouldSkip(user, category)) {
            return;
        }
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedEmailId(relatedEmailId)
                .build();
        notificationRepository.save(notification);
    }

    private boolean shouldSkip(User user, String category) {
        if (user == null) {
            return false;
        }
        if (inQuietHours(user)) {
            return true;
        }
        return isCategoryMuted(user, category);
    }

    private static boolean inQuietHours(User user) {
        Integer start = user.getQuietHoursStart();
        Integer end = user.getQuietHoursEnd();
        if (start == null || end == null) {
            return false;
        }
        int hour = LocalDateTime.now().getHour();
        if (start.equals(end)) {
            return false;
        }
        if (start < end) {
            return hour >= start && hour < end;
        }
        // wraps midnight, e.g. 22 -> 7
        return hour >= start || hour < end;
    }

    private static boolean isCategoryMuted(User user, String category) {
        if (category == null || user.getMutedCategories() == null || user.getMutedCategories().isBlank()) {
            return false;
        }
        Set<String> muted = Arrays.stream(user.getMutedCategories().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return muted.contains(category.toUpperCase(Locale.ROOT));
    }
}
