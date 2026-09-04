package com.nexora.service;

import com.nexora.dto.response.CortexScoreResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.exception.NexoraException;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Deterministic Cortex Score (0-100) for the signed-in mailbox only.
 * Uses Gmail label counts plus inbox deadlines and actions. Never invents mail.
 */
@Service
public class CortexScoreService {

    private final GmailSyncService gmailSyncService;
    private final EmailActionRepository actionRepository;
    private final EmailRepository emailRepository;

    public CortexScoreService(GmailSyncService gmailSyncService,
                              EmailActionRepository actionRepository,
                              EmailRepository emailRepository) {
        this.gmailSyncService = gmailSyncService;
        this.actionRepository = actionRepository;
        this.emailRepository = emailRepository;
    }

    public CortexScoreResponse compute(Long userId) {
        if (userId == null) {
            throw new NexoraException("Unauthorized", 401);
        }

        long localInbox = emailRepository.countByUserIdAndInInboxTrue(userId);
        if (localInbox == 0) {
            return CortexScoreResponse.pending(
                    "Sync Gmail first",
                    "Sync Gmail to score this inbox. Nothing is invented before mail is stored.");
        }

        Map<String, GmailLabelCountResponse> labels = labelCountsOrEmpty(userId);
        GmailLabelCountResponse inboxLabel = labels.get("INBOX");
        boolean hasGmailSignals = inboxLabel != null && inboxLabel.getMessagesTotal() != null;

        long unread;
        if (hasGmailSignals) {
            unread = gmailSyncService.getInboxUnreadCount(userId);
        } else {
            unread = emailRepository.countInboxUnreadByUserId(userId);
        }
        long importantUnread = unreadOf(labels.get("IMPORTANT"));
        long starredUnread = unreadOf(labels.get("STARRED"));
        long pendingActions = actionRepository.countOpenInboxFollowUps(userId);

        LocalDateTime now = LocalDateTime.now();
        long overdue = emailRepository.countOverdueDeadlines(userId, now);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);
        long meetingsToday = emailRepository.countTodaysMeetings(userId, todayStart, todayEnd);

        List<CortexScoreResponse.Factor> factors = new ArrayList<CortexScoreResponse.Factor>();
        factors.add(factor("unread", "Unread", -Math.min(40, unread * 1.2), unreadDetail(unread)));
        factors.add(factor("important", "Flagged unread", -Math.min(15, importantUnread * 2.0),
                importantUnread == 0 ? "No flagged unread" : importantUnread + " unread marked important"));
        factors.add(factor("starred", "Starred unread", -Math.min(10, starredUnread * 1.5),
                starredUnread == 0 ? "No starred unread" : starredUnread + " starred messages still unread"));
        factors.add(factor("actions", "Follow-ups", -Math.min(25, pendingActions * 3.0),
                pendingActions == 0 ? "No open follow-ups from mail" : pendingActions + " follow-ups still open"));
        factors.add(factor("overdue", "Overdue dates", -Math.min(20, overdue * 5.0),
                overdue == 0 ? "No past-due dates in inbox" : overdue + " inbox messages with a date that already passed"));
        factors.add(factor("meetings", "Meetings today", -Math.min(10, meetingsToday * 2.0),
                meetingsToday == 0 ? "No meeting mail due today" : meetingsToday + " meeting messages due today"));

        int factorSum = 0;
        for (int i = 0; i < factors.size(); i++) {
            factorSum += factors.get(i).getPoints();
        }
        int score = Math.max(0, Math.min(100, 100 + factorSum));
        String next = nextAction(unread, importantUnread, starredUnread, pendingActions, overdue, meetingsToday);

        CortexScoreResponse response = new CortexScoreResponse();
        response.setReady(true);
        response.setScore(score);
        response.setBand(bandFor(score));
        response.setFactors(factors);
        response.setNextAction(next);
        response.setStatusMessage(next);
        return response;
    }

    private static String unreadDetail(long unread) {
        if (unread == 0) {
            return "Inbox unread is clear";
        }
        return unread + " unread in Gmail Inbox";
    }

    private Map<String, GmailLabelCountResponse> labelCountsOrEmpty(Long userId) {
        try {
            Map<String, GmailLabelCountResponse> labels = gmailSyncService.getLabelCounts(userId);
            if (labels != null) {
                return labels;
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private static long unreadOf(GmailLabelCountResponse label) {
        if (label != null && label.getMessagesUnread() != null) {
            return label.getMessagesUnread();
        }
        return 0L;
    }

    private static CortexScoreResponse.Factor factor(String key, String label, double points, String detail) {
        CortexScoreResponse.Factor item = new CortexScoreResponse.Factor();
        item.setKey(key);
        item.setLabel(label);
        item.setPoints((int) Math.round(points));
        item.setDetail(detail);
        return item;
    }

    private static String nextAction(long unread, long importantUnread, long starredUnread,
                                     long pendingActions, long overdue, long meetingsToday) {
        if (overdue == 1) {
            return "Open the overdue message on Home and handle the date first";
        }
        if (overdue > 1) {
            return "Clear " + overdue + " overdue dates on Home before the rest of the inbox";
        }
        if (importantUnread > 0) {
            return "Open Inbox Flagged and work the " + importantUnread + " unread marked important";
        }
        if (starredUnread > 0) {
            return "Open Inbox Starred and read the " + starredUnread + " you starred";
        }
        if (meetingsToday > 0) {
            return "Check todays meeting mail on Home before the day fills up";
        }
        if (pendingActions == 1) {
            return "Finish the open follow-up listed on Home";
        }
        if (pendingActions > 1) {
            return "Finish " + pendingActions + " open follow-ups on Home";
        }
        if (unread == 1) {
            return "One unread left. Open Primary or tap Mark all as read";
        }
        if (unread > 1) {
            return unread + " unread. Start in Primary, or Mark all as read when you are caught up";
        }
        return "Inbox looks clear. Sync if new mail should be here.";
    }

    private static String bandFor(int score) {
        if (score >= 85) {
            return "Clear";
        }
        if (score >= 70) {
            return "Light";
        }
        if (score >= 50) {
            return "Moderate";
        }
        if (score >= 30) {
            return "Heavy";
        }
        return "Critical";
    }
}
