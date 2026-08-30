package com.nexora.service;

import com.nexora.dto.response.CortexScoreResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.User;
import com.nexora.repository.EmailActionRepository;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CortexScoreServiceTest {

    @Mock GmailSyncService gmailSyncService;
    @Mock EmailActionRepository actionRepository;
    @Mock EmailRepository emailRepository;
    @Mock UserRepository userRepository;

    CortexScoreService service;

    @BeforeEach
    void setUp() {
        service = new CortexScoreService(gmailSyncService, actionRepository, emailRepository, userRepository);
    }

    @Test
    void noInboxMailIsPendingNotPerfect() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertFalse(result.isReady());
        assertNull(result.getScore());
        assertEquals("Sync Gmail first", result.getBand());
    }

    @Test
    void unclassifiedInboxIsPending() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(10L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(4L);

        CortexScoreResponse result = service.compute(1L);

        assertFalse(result.isReady());
        assertNull(result.getScore());
        assertEquals("Classifying", result.getBand());
    }

    @Test
    void waitsForSecondaryEnrichmentWhileSyncRunning() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(10L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(gmailSyncService.hasActiveSync(1L)).thenReturn(true);

        CortexScoreResponse result = service.compute(1L);

        assertFalse(result.isReady());
        assertEquals("Enriching", result.getBand());
    }

    @Test
    void doesNotStickOnEnrichingWhenSyncIdleWithoutHistory() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(10L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(gmailSyncService.hasActiveSync(1L)).thenReturn(false);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of());

        CortexScoreResponse result = service.compute(1L);

        assertFalse(result.isReady());
        assertEquals("Awaiting Gmail", result.getBand());
    }

    @Test
    void scoresWhenIdleWithoutHistoryIfLabelsAvailable() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(8L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(User.builder().id(1L).build()));
        when(gmailSyncService.hasActiveSync(1L)).thenReturn(false);
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 8L, 2L, 8L, 2L)
        ));
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(2L);
        when(actionRepository.findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(1L)).thenReturn(List.of());
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(0L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertNotNull(result.getScore());
        assertNotEquals("Enriching", result.getBand());
    }

    @Test
    void scoredInboxStaysInRangeAndFactorsMatch() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(20L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).gmailHistoryId("12345").build()));
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 20L, 5L, 20L, 5L),
                "IMPORTANT", new GmailLabelCountResponse("IMPORTANT", "IMPORTANT", "system", 2L, 1L, 2L, 1L)
        ));
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(5L);
        when(actionRepository.findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(1L)).thenReturn(List.of());
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(0L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertNotNull(result.getScore());
        assertTrue(result.getScore() >= 0 && result.getScore() <= 100);
        int factorSum = 0;
        for (CortexScoreResponse.Factor factor : result.getFactors()) {
            factorSum += factor.getPoints();
        }
        assertEquals(100 + factorSum, result.getScore());
    }

    @Test
    void overdueDeadlinesReduceScore() {
        when(emailRepository.countByUserIdAndInInboxTrue(1L)).thenReturn(5L);
        when(emailRepository.countByUserIdAndCategoryAndInInboxTrue(1L, EmailCategory.UNCATEGORIZED))
                .thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).gmailHistoryId("12345").build()));
        when(gmailSyncService.getLabelCounts(1L)).thenReturn(Map.of(
                "INBOX", new GmailLabelCountResponse("INBOX", "INBOX", "system", 5L, 0L, 5L, 0L)
        ));
        when(gmailSyncService.getInboxUnreadCount(1L)).thenReturn(0L);
        when(actionRepository.findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(1L)).thenReturn(List.of());
        when(emailRepository.countOverdueDeadlines(eq(1L), any())).thenReturn(1L);
        when(emailRepository.countTodaysMeetings(eq(1L), any(), any())).thenReturn(0L);

        CortexScoreResponse result = service.compute(1L);

        assertTrue(result.isReady());
        assertTrue(result.getScore() < 100);
    }
}
