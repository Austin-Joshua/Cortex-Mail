package com.nexora.service;

import com.nexora.dto.response.CortexScoreResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.exception.NexoraException;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CortexScoreServiceTest {

    @Mock GmailSyncService gmailSyncService;
    @Mock EmailActionRepository actionRepository;
    @Mock EmailRepository emailRepository;

    CortexScoreService service;

    @BeforeEach
    void setUp() {
        service = new CortexScoreService(gmailSyncService, actionRepository, emailRepository);
    }

    @Test
    void rejectsMissingUser() {
        NexoraException ex = assertThrows(NexoraException.class, () -> service.compute(null));
        assertEquals(401, ex.getStatusCode());
    }

    @Test
    void noInboxMailIsPendingNotPerfect() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertFalse(result.isReady());
        assertNull(result.getScore());
        assertEquals("Sync Gmail first", result.getBand());
        assertNull(result.getNextAction());
    }

    @Test
    void scoresImmediatelyWhenInboxExistsEvenIfStillGrouping() {
        stubHealthyInbox(10L, 4L, Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 10L, 4L, 10L, 4L)
        ));

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertNotNull(result.getScore());
        assertNotNull(result.getNextAction());
        assertTrue(result.getNextAction().toLowerCase().contains("unread"));
    }

    @Test
    void scoresFromLocalUnreadWhenGmailLabelsMissing() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(8L);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of());
        when(emailRepository.countInboxUnreadByUserId(1L)).thenReturn(3L);
        when(actionRepository.countOpenInboxFollowUps(1L)).thenReturn(0L);
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(0L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertNotNull(result.getScore());
        assertNotEquals("Awaiting Gmail", result.getBand());
        assertNotEquals("Enriching", result.getBand());
    }

    @Test
    void scoredInboxStaysInRangeAndFactorsMatch() {
        stubHealthyInbox(20L, 5L, Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 20L, 5L, 20L, 5L),
                "IMPORTANT", new GmailLabelCountResponse("IMPORTANT", "IMPORTANT", "system", 2L, 1L, 2L, 1L)
        ));

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertNotNull(result.getScore());
        assertTrue(result.getScore() >= 0 && result.getScore() <= 100);
        int factorSum = 0;
        for (CortexScoreResponse.Factor factor : result.getFactors()) {
            factorSum += factor.getPoints();
        }
        assertEquals(100 + factorSum, result.getScore());
        assertFalse(result.getFactors().stream().anyMatch(f -> f.getDetail().contains("deadlineDetected")));
    }

    @Test
    void overdueDeadlinesReduceScoreAndLeadNextAction() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(5L);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 5L, 0L, 5L, 0L)
        ));
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(0L);
        when(actionRepository.countOpenInboxFollowUps(1L)).thenReturn(0L);
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(1L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertTrue(result.getScore() < 100);
        assertTrue(result.getNextAction().toLowerCase().contains("overdue"));
    }

    @Test
    void inboxFollowUpsReduceScoreAndLeadWhenInboxIsOtherwiseClear() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(4L);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 4L, 0L, 4L, 0L)
        ));
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(0L);
        when(actionRepository.countOpenInboxFollowUps(1L)).thenReturn(2L);
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(0L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertEquals(94, result.getScore());
        assertTrue(result.getNextAction().toLowerCase().contains("follow-up"));
        int factorSum = result.getFactors().stream().mapToInt(CortexScoreResponse.Factor::getPoints).sum();
        assertEquals(100 + factorSum, result.getScore());
    }

    @Test
    void roundedFactorsAlwaysAddUpToScore() {
        stubHealthyInbox(10L, 3L, Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 10L, 3L, 10L, 3L),
                "STARRED", new GmailLabelCountResponse("STARRED", "STARRED", "system", 1L, 1L, 1L, 1L)
        ));

        CortexScoreResponse result = service.compute(1L);

        int factorSum = result.getFactors().stream().mapToInt(CortexScoreResponse.Factor::getPoints).sum();
        assertEquals(100 + factorSum, result.getScore());
        assertTrue(result.getScore() < 100);
    }

    private void stubHealthyInbox(long inbox, long unread, Map<String, GmailLabelCountResponse> labels) {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(inbox);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(labels);
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(unread);
        when(actionRepository.countOpenInboxFollowUps(1L)).thenReturn(0L);
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(0L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);
    }
}
