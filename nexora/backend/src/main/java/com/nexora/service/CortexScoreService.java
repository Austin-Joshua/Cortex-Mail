package com.nexora.service;

import com.nexora.dto.response.CortexScoreResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.EmailAction;
import com.nexora.model.User;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic, explainable Cortex Score (0–100).
 * Gmail-native signals + Cortex-derived actions/deadlines only — never invents mail state.
 */
@Service
@RequiredArgsConstructor
public class CortexScoreService {

    private final GmailSyncService gmailSyncService;
    private final EmailActionRepository actionRepository;
    private final EmailRepository emailRepository;
    private final UserRepository userRepository;

    public CortexScoreResponse compute(Long userId) {
        long localInbox = emailRepository.countByUserIdAndInInboxTrue(userId);
        if (localInbox == 0) {
            return scorePending("Sync Gmail first", "Connect and sync Gmail — no score is shown until real inbox mail is stored.");
        }

        long unclassified = emailRepository.countByUserIdAndCategoryAndInInboxTrue(
                userId, EmailCategory.UNCATEGORIZED);
        if (unclassified > 0) {
            return scorePending(
                    "Classifying",
                    unclassified + " inbox messages still being analyzed — score appears when classification finishes.");
        }

        User user = userRepository.findById(userId).orElse(null);
        boolean secondaryComplete = user != null
                && SyncStatusFlags.secondaryComplete(user.getGmailHistoryId());
        // Only hold the score for enrichment while a sync is actually running —
        // never leave Cortex Score on "Enriching…" forever after a failed secondary pass.
        if (SyncStatusFlags.enrichingInBackground(true, secondaryComplete, gmailSyncService.hasActiveSync(userId))) {
            return scorePending(
                    "Enriching",
                    "Finishing full message sync so actions and deadlines are included in your score.");
        }

        Map<String, GmailLabelCountResponse> labels = gmailSyncService.getLabelCounts(userId);
        GmailLabelCountResponse inboxLabel = labels.get("INBOX");
        if (inboxLabel == null || inboxLabel.getMessagesTotal() == null) {
            return scorePending("Awaiting Gmail", "Waiting for Gmail label counts before scoring.");
        }

        long unread = gmailSyncService.getInboxUnreadCount(userId);
        long importantUnread = 0;
        if (labels.get("IMPORTANT") != null && labels.get("IMPORTANT").getMessagesUnread() != null) {
            importantUnread = labels.get("IMPORTANT").getMessagesUnread();
        }

        List<EmailAction> pending = actionRepository.findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(userId);
        LocalDateTime now = LocalDateTime.now();
        long overdue = emailRepository.countOverdueDeadlines(userId, now);
        LocalDateTime todayStart = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime todayEnd = now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
        long meetingsToday = emailRepository.countTodaysMeetings(userId, todayStart, todayEnd);

        List<CortexScoreResponse.Factor> factors = new ArrayList<>();

        double unreadDebit = Math.min(40, unread * 1.2);
        factors.add(factor("unread", "Gmail INBOX unread", -unreadDebit,
                unread + " unread messages in Gmail INBOX"));

        double importantDebit = Math.min(15, importantUnread * 2.0);
        factors.add(factor("important", "Gmail IMPORTANT unread", -importantDebit,
                importantUnread + " unread messages labeled IMPORTANT"));

        double actionDebit = Math.min(25, pending.size() * 3.0);
        factors.add(factor("actions", "Unresolved actions", -actionDebit,
                pending.size() + " open action items extracted from mail"));

        double overdueDebit = Math.min(20, overdue * 5.0);
        factors.add(factor("overdue", "Overdue deadlines", -overdueDebit,
                overdue + " emails with past deadlineDetected"));

        double meetingDebit = Math.min(10, meetingsToday * 2.0);
        factors.add(factor("meetings", "Today's meetings", -meetingDebit,
                meetingsToday + " meeting-category emails due today"));

        double raw = 100 - unreadDebit - importantDebit - actionDebit - overdueDebit - meetingDebit;
        int score = (int) Math.max(0, Math.min(100, Math.round(raw)));
        String band = bandFor(score);

        return CortexScoreResponse.builder()
                .ready(true)
                .score(score)
                .band(band)
                .factors(factors)
                .build();
    }

    private CortexScoreResponse scorePending(String band, String message) {
        return CortexScoreResponse.pending(band, message);
    }

    private static CortexScoreResponse.Factor factor(String key, String label, double points, String detail) {
        return CortexScoreResponse.Factor.builder()
                .key(key)
                .label(label)
                .points((int) Math.round(points))
                .detail(detail)
                .build();
    }

    /** Higher score = healthier inbox (fewer debit factors). */
    private static String bandFor(int score) {
        if (score >= 85) return "Clear";
        if (score >= 70) return "Light";
        if (score >= 50) return "Moderate";
        if (score >= 30) return "Heavy";
        return "Critical";
    }
}
