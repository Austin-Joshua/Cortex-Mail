package com.nexora.service;

import com.nexora.dto.response.DashboardSummaryResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.model.Email;
import com.nexora.model.EmailAction;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final EmailRepository emailRepository;
    private final EmailActionRepository actionRepository;
    private final EmailService emailService;
    private final GmailSyncService gmailSyncService;

    public DashboardSummaryResponse getSummary(Long userId) {
        // Priority emails (top 5 HIGH unread)
        List<Email> highPriority = emailRepository
                .findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
                        userId, Email.Priority.HIGH, PageRequest.of(0, 5));

        // Upcoming deadlines (next 7 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekOut = now.plusDays(7);
        List<Email> deadlines = emailRepository.findUpcomingDeadlines(userId, now, weekOut);

        // Pending action items
        List<EmailAction> pendingActions = actionRepository
                .findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(userId);

        // Unread count from Gmail INBOX label (matches real Gmail account)
        long unreadCount = gmailSyncService.getInboxUnreadCount(userId);
        Map<String, GmailLabelCountResponse> gmailLabelCounts = gmailSyncService.getLabelCounts(userId);

        // Category counts
        Map<String, Long> categoryCounts = emailService.getCategoryCounts(userId);

        // Today's meetings
        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59).withNano(999999999);
        List<Email> todaysMeetings = emailRepository.findTodaysMeetings(userId, todayStart, todayEnd);

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
