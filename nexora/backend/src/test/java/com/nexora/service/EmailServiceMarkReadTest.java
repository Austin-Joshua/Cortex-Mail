package com.nexora.service;

import com.nexora.model.Email;
import com.nexora.repository.EmailRepository;
import com.nexora.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceMarkReadTest {

    @Mock EmailRepository emailRepository;
    @Mock UserRepository userRepository;
    @Mock GmailSyncService gmailSyncService;
    @Mock EmailClassificationService classificationService;
    @Mock PostSyncProcessingService postSyncProcessingService;

    @InjectMocks EmailService emailService;

    @Test
    void markReadShortCircuitsWhenAlreadyRead() {
        Email email = Email.builder()
                .id(9L)
                .isRead(true)
                .gmailMessageId("msg-1")
                .subject("Hello")
                .build();
        when(emailRepository.findOwnedByIdAndUserId(9L, 1L)).thenReturn(Optional.of(email));

        var response = emailService.markRead(1L, 9L);

        assertTrue(Boolean.TRUE.equals(response.getIsRead()));
        verify(gmailSyncService, never()).markReadInGmail(any(), any());
        verify(emailRepository, never()).save(any());
    }
}
