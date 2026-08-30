package com.nexora.service;

import com.nexora.model.OauthExchangeCode;
import com.nexora.repository.OauthExchangeCodeRepository;
import com.nexora.security.TokenEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OauthExchangeServiceTest {

    @Mock
    private OauthExchangeCodeRepository repository;

    private TokenEncryptor tokenEncryptor;
    private OauthExchangeService service;

    @BeforeEach
    void setUp() {
        tokenEncryptor = new TokenEncryptor();
        ReflectionTestUtils.setField(tokenEncryptor, "encryptionKey", "0123456789abcdef");
        service = new OauthExchangeService(repository, tokenEncryptor);
    }

    @Test
    void storeDoesNotPersistRawJwt() {
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String code = service.store(42L, true);

        assertNotNull(code);
        assertFalse(code.contains("."));
        ArgumentCaptor<OauthExchangeCode> captor = ArgumentCaptor.forClass(OauthExchangeCode.class);
        verify(repository).save(captor.capture());
        String payload = captor.getValue().getPayload();
        assertNotNull(payload);
        assertFalse(payload.contains("eyJ"), "payload must not contain a JWT");
        assertEquals("42;true", tokenEncryptor.decrypt(payload));
    }

    @Test
    void consumeReturnsUserPayloadOnce() {
        String encrypted = tokenEncryptor.encrypt("7;false");
        OauthExchangeCode row = OauthExchangeCode.builder()
                .code("abc")
                .payload(encrypted)
                .expiresAt(LocalDateTime.now().plusMinutes(1))
                .build();
        when(repository.findById("abc")).thenReturn(Optional.of(row));

        Optional<OauthExchangeService.ExchangePayload> first = service.consume("abc");
        assertTrue(first.isPresent());
        assertEquals(7L, first.get().userId());
        assertFalse(first.get().onboardingComplete());
        verify(repository).delete(row);
    }

    @Test
    void consumeRejectsExpired() {
        String encrypted = tokenEncryptor.encrypt("7;true");
        OauthExchangeCode row = OauthExchangeCode.builder()
                .code("old")
                .payload(encrypted)
                .expiresAt(LocalDateTime.now().minusSeconds(5))
                .build();
        when(repository.findById("old")).thenReturn(Optional.of(row));

        assertTrue(service.consume("old").isEmpty());
    }
}
