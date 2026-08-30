package com.nexora.service;

import com.nexora.dto.response.DashboardSummaryResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
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
        List<Email> highPriority = emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Email.Priority.HIGH, PageRequest.of(0, 5));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekOut = now.plusDays(7);
        List<Email> deadlines = emailRepository.findUpcomingDeadlines(userId, now, weekOut);

        List<EmailAction> pendingActions = actionRepository
                .findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(userId);

        long unreadCount = gmailSyncService.getInboxUnreadCount(userId);
        Map<String, GmailLabelCountResponse> gmailLabelCounts = gmailSyncService.getLabelCounts(userId);
        Map<String, Long> categoryCounts = emailService.getCategoryCounts(userId);

        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        // Cap meeting list for summary payload — score uses COUNT separately.
        List<Email> todaysMeetings = emailRepository.findTodaysMeetings(userId, todayStart, todayEnd)
                .stream()
                .limit(10)
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
                .categoryCounts(categoryCounts)
                .gmailLabelCounts(gmailLabelCounts)
                .todaysMeetings(todaysMeetings.stream()
                        .map(e -> emailService.toResponse(e, false))
                        .collect(Collectors.toList()))
                .cortexScore(cortexScoreService.compute(userId))
                .build();
    }

    private DashboardSummaryResponse.ActionItemResponse toActionResponse(EmailAction a) {
        return DashboardSummaryResponse.ActionItemResponse.builder()
                .id(a.getId())
                .emailId(a.getEmail() != null ? a.getEmail().getId() : null)
                .emailSubject(a.getEmail() != null ? a.getEmail().getSubject() : null)
                .senderName(a.getEmail() != null ? a.getEmail().getSenderName() : null)
                .actionType(a.getActionType().name())
                .actionDescription(a.getActionDescription())
                .deadline(a.getDeadline() != null ? a.getDeadline().toString() : null)
                .isCompleted(a.getIsCompleted())
                .build();
    }
}
