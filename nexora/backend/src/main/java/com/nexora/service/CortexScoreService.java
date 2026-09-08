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
 * Deterministic Cortex Score (0-100) for the signed-in mailbox.
 * Uses Gmail inbox unread, stored mail, and recent inbox dates. Large
 * mailboxes still get a visible number — penalties taper instead of
 * stacking to zero.
 */
@Service
public class CortexScoreService {

    private static final int OVERDUE_LOOKBACK_DAYS = 14;

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

        long stored = emailRepository.countByUserId(userId);
        long localInbox = emailRepository.countByUserIdAndInInboxTrue(userId);
        if (stored == 0 && localInbox == 0) {
            return CortexScoreResponse.pending(
                    "Sync Gmail first",
                    "Sync Gmail to score this mailbox. Nothing is invented before mail is stored.");
        }

        Map<String, GmailLabelCountResponse> labels = labelCountsOrEmpty(userId);
        GmailLabelCountResponse inboxLabel = labels.get("INBOX");
        boolean hasGmailSignals = inboxLabel != null && inboxLabel.getMessagesUnread() != null;

        long unread;
        if (hasGmailSignals) {
            unread = gmailSyncService.getInboxUnreadCount(userId);
        } else {
            unread = emailRepository.countInboxUnreadByUserId(userId);
        }
        long importantUnread = unreadOf(labels.get("IMPORTANT"));
        long starredUnread = unreadOf(labels.get("STARRED"));
        long pendingActions = actionRepository.countOpenInboxFollowUps(userId, LocalDateTime.now());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime overdueSince = now.minusDays(OVERDUE_LOOKBACK_DAYS);
        long overdue = emailRepository.countOverdueDeadlines(userId, now, overdueSince);
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime todayEnd = now.toLocalDate().atTime(23, 59, 59);
        long meetingsToday = emailRepository.countTodaysMeetings(userId, todayStart, todayEnd);

        List<CortexScoreResponse.Factor> factors = new ArrayList<CortexScoreResponse.Factor>();
        factors.add(factor("stored", "Stored mail", 0,
                stored + " messages stored from this Gmail account"));
        factors.add(factor("unread", "Inbox unread", unreadPoints(unread), unreadDetail(unread)));
        factors.add(factor("important", "Flagged unread", capped(-importantUnread * 2.0, 12),
                importantUnread == 0 ? "No flagged unread" : importantUnread + " unread marked important"));
        factors.add(factor("starred", "Starred unread", capped(-starredUnread * 1.5, 8),
                starredUnread == 0 ? "No starred unread" : starredUnread + " starred messages still unread"));
        factors.add(factor("actions", "Follow-ups", capped(-pendingActions * 3.0, 18),
                pendingActions == 0 ? "No open follow-ups from mail" : pendingActions + " follow-ups still open"));
        factors.add(factor("overdue", "Overdue dates", capped(-overdue * 4.0, 15),
                overdue == 0
                        ? "No dates missed in the last " + OVERDUE_LOOKBACK_DAYS + " days"
                        : overdue + " inbox dates in the last " + OVERDUE_LOOKBACK_DAYS + " days already passed"));
        factors.add(factor("meetings", "Meetings today", capped(-meetingsToday * 2.0, 8),
                meetingsToday == 0 ? "No meeting mail due today" : meetingsToday + " meeting messages due today"));

        int factorSum = 0;
        for (int i = 0; i < factors.size(); i++) {
            factorSum += factors.get(i).getPoints();
        }
        int score = Math.max(0, Math.min(100, 100 + factorSum));
        List<CortexScoreResponse.NextActionItem> nextActions = buildNextActions(
                unread, importantUnread, starredUnread, pendingActions, overdue, meetingsToday);
        String next = nextActions.isEmpty()
                ? "Inbox looks clear. Sync if new mail should be here."
                : nextActions.get(0).getLabel();

        CortexScoreResponse response = new CortexScoreResponse();
        response.setReady(true);
        response.setScore(score);
        response.setBand(bandFor(score));
        response.setFactors(factors);
        response.setNextAction(next);
        response.setNextActions(nextActions);
        response.setStatusMessage(next);
        response.setInboxUnread(unread);
        response.setOverdueCount(overdue);
        response.setStoredCount(stored);
        return response;
    }

    /** Log curve so 4k unread costs about the same as a few hundred, not a hard zero. */
    static int unreadPoints(long unread) {
        if (unread <= 0) {
            return 0;
        }
        return (int) -Math.round(Math.min(28.0, 10.0 * Math.log10(1.0 + unread)));
    }

    private static int capped(double points, int maxAbs) {
        return (int) Math.round(Math.max(-maxAbs, Math.min(0, points)));
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

    private static CortexScoreResponse.Factor factor(String key, String label, int points, String detail) {
        CortexScoreResponse.Factor item = new CortexScoreResponse.Factor();
        item.setKey(key);
        item.setLabel(label);
        item.setPoints(points);
        item.setDetail(detail);
        return item;
    }

    private static List<CortexScoreResponse.NextActionItem> buildNextActions(
            long unread, long importantUnread, long starredUnread,
            long pendingActions, long overdue, long meetingsToday) {
        List<CortexScoreResponse.NextActionItem> items = new ArrayList<>();
        if (overdue > 0) {
            items.add(nextItem("OVERDUE",
                    overdue == 1
                            ? "Open Deadlines and handle the overdue date first"
                            : "Clear " + overdue + " overdue dates on Deadlines before the rest of the inbox",
                    "/scheduled", overdue));
        }
        if (importantUnread > 0) {
            items.add(nextItem("FLAGGED",
                    "Open Inbox Flagged and work the " + importantUnread + " unread marked important",
                    "/inbox?view=IMPORTANT", importantUnread));
        }
        if (starredUnread > 0) {
            items.add(nextItem("STARRED",
                    "Open Inbox Starred and read the " + starredUnread + " you starred",
                    "/inbox?view=STARRED", starredUnread));
        }
        if (meetingsToday > 0) {
            items.add(nextItem("MEETINGS",
                    "Review today's meeting mail on Deadlines before the day fills up",
                    "/scheduled", meetingsToday));
        }
        if (pendingActions > 0) {
            items.add(nextItem("FOLLOW_UPS",
                    pendingActions == 1
                            ? "Finish the open follow-up in Triage"
                            : "Finish " + pendingActions + " open follow-ups in Triage",
                    "/triage", pendingActions));
        }
        if (unread > 0) {
            items.add(nextItem("UNREAD",
                    unread == 1
                            ? "One unread left. Open Primary or tap Mark all as read"
                            : unread + " unread. Start in Primary, or Mark all as read when you are caught up",
                    "/inbox", unread));
        }
        if (items.isEmpty()) {
            items.add(nextItem("CLEAR",
                    "Inbox looks clear. Sync if new mail should be here.",
                    "/inbox", 0L));
        }
        return items;
    }

    private static CortexScoreResponse.NextActionItem nextItem(String type, String label, String route, long count) {
        CortexScoreResponse.NextActionItem item = new CortexScoreResponse.NextActionItem();
        item.setType(type);
        item.setLabel(label);
        item.setRoute(route);
        item.setCount(count);
        return item;
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
