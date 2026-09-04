package com.nexora.service;

import com.nexora.dto.response.DashboardSummaryResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.exception.NexoraException;
import com.nexora.model.Email;
import com.nexora.model.EmailAction;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DashboardService {

    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;
    private final EmailService emailService;
    private final GmailSyncService gmailSyncService;
    private final CortexScoreService cortexScoreService;

    public DashboardService(EmailRepository emailRepository,
                            EmailActionRepository actionRepository,
                            EmailService emailService,
                            GmailSyncService gmailSyncService,
                            CortexScoreService cortexScoreService) {
        this.emailRepository = emailRepository;
        this.actionRepository = actionRepository;
        this.emailService = emailService;
        this.gmailSyncService = gmailSyncService;
        this.cortexScoreService = cortexScoreService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        if (userId == null) {
            throw new NexoraException("Unauthorized", 401);
        }

        List<Email> highPriority = emailRepository
                .findByUserIdAndInInboxTrueAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Email.Priority.HIGH, PageRequest.of(0, 6));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekOut = now.plusDays(7);
        List<Email> deadlines = emailRepository.findUpcomingDeadlines(userId, now, weekOut)
                .stream()
                .limit(8)
                .collect(Collectors.toList());

        List<EmailAction> pendingActions = actionRepository
                .findOpenInboxFollowUps(userId, PageRequest.of(0, 8));

        Map<String, GmailLabelCountResponse> gmailLabelCounts = labelCountsOrEmpty(userId);
        long unreadCount = unreadCountOrLocal(userId, gmailLabelCounts);
        Map<String, Long> categoryCounts = emailService.getCategoryCounts(userId);

        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        List<Email> todaysMeetings = emailRepository.findTodaysMeetings(userId, todayStart, todayEnd)
                .stream()
                .limit(8)
                .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
                .priorityEmails(highPriority.stream()
                        .map(e -> emailService.toResponse(e, false))
                        .collect(Collectors.toList()))
                .upcomingDeadlines(deadlines.stream()
                        .map(e -> emailService.toResponse(e, false))
                        .collect(Collectors.toList()))
                .pendingActions(pendingActions.stream()
                        .map(this::toActionResponse)
                        .collect(Collectors.toList()))
                .unreadCount(unreadCount)
                .storedEmailCount(emailRepository.countByUserId(userId))
                .categoryCounts(categoryCounts)
                .gmailLabelCounts(gmailLabelCounts)
                .todaysMeetings(todaysMeetings.stream()
                        .map(e -> emailService.toResponse(e, false))
                        .collect(Collectors.toList()))
                .cortexScore(cortexScoreService.compute(userId))
                .build();
    }

    private Map<String, GmailLabelCountResponse> labelCountsOrEmpty(Long userId) {
        try {
            Map<String, GmailLabelCountResponse> labels = gmailSyncService.getLabelCounts(userId);
            return labels != null ? labels : Map.of();
        } catch (Exception e) {
            log.warn("Home used local counts — Gmail label cache unavailable for user {}", userId);
            return Map.of();
        }
    }

    private long unreadCountOrLocal(Long userId, Map<String, GmailLabelCountResponse> labels) {
        GmailLabelCountResponse inbox = labels.get("INBOX");
        if (inbox != null && inbox.getMessagesUnread() != null) {
            return inbox.getMessagesUnread();
        }
        return emailRepository.countInboxUnreadByUserId(userId);
    }

    private DashboardSummaryResponse.ActionItemResponse toActionResponse(EmailAction a) {
        Email email = a.getEmail();
        return DashboardSummaryResponse.ActionItemResponse.builder()
                .id(a.getId())
                .emailId(email != null ? email.getId() : null)
                .emailSubject(email != null ? email.getSubject() : null)
                .senderName(email != null ? email.getSenderName() : null)
                .actionType(a.getActionType() != null ? a.getActionType().name() : "REVIEW")
                .actionDescription(a.getActionDescription())
                .deadline(a.getDeadline() != null ? a.getDeadline().toString() : null)
                .isCompleted(a.getIsCompleted())
                .build();
    }
}
