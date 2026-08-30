package com.nexora.service;

import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.EmailAction;
import com.nexora.model.Notification;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;

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
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tomorrow = now.plusDays(1);

        // Check deadlines today or tomorrow
        List<EmailAction> urgentActions = actionRepository
                .findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(userId, now, tomorrow);

        LocalDateTime sinceMidnight = now.toLocalDate().atStartOfDay();
        for (EmailAction action : urgentActions) {
            if (Boolean.TRUE.equals(action.getIsCompleted())) continue;
            Long emailId = action.getEmail() != null ? action.getEmail().getId() : null;
            if (emailId != null && notificationRepository.existsByUserIdAndRelatedEmailIdAndNotificationTypeAndCreatedAtAfter(
                    userId, emailId, Notification.NotificationType.DEADLINE, sinceMidnight)) {
                continue;
            }
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title("Deadline soon")
                    .message(action.getActionDescription() + " — due " + action.getDeadline())
                    .notificationType(Notification.NotificationType.DEADLINE)
                    .relatedEmailId(emailId)
                    .build();
            notificationRepository.save(notification);
        }

        // Check unread HIGH priority emails in last 24h
        LocalDateTime since = now.minusHours(24);
        List<Email> highPriorityEmails = emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Email.Priority.HIGH,
                        org.springframework.data.domain.PageRequest.of(0, 5));

        for (Email email : highPriorityEmails) {
            if (email.getReceivedAt() == null || !email.getReceivedAt().isAfter(since)) continue;
            if (notificationRepository.existsByUserIdAndRelatedEmailIdAndNotificationTypeAndCreatedAtAfter(
                    userId, email.getId(), Notification.NotificationType.IMPORTANT_EMAIL, sinceMidnight)) {
                continue;
            }
            Notification notification = Notification.builder()
                    .userId(userId)
                    .title("Important email")
                    .message("High priority email from " + email.getSenderName() + ": " + email.getSubject())
                    .notificationType(Notification.NotificationType.IMPORTANT_EMAIL)
                    .relatedEmailId(email.getId())
                    .build();
            notificationRepository.save(notification);
        }
    }

    public void createNotification(Long userId, String title, String message,
                                    Notification.NotificationType type, Long relatedEmailId) {
        Notification notification = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .notificationType(type)
                .relatedEmailId(relatedEmailId)
                .build();
        notificationRepository.save(notification);
    }
}
