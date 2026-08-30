package com.nexora.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class TokenEncryptorTest {

    private TokenEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new TokenEncryptor();
        ReflectionTestUtils.setField(encryptor, "encryptionKey", "0123456789abcdef");
        encryptor.validateKey();
    }

    @Test
    void roundTripGcm() {
        String plain = "ya29.gmail-token-secret";
        String sealed = encryptor.encrypt(plain);
        assertNotNull(sealed);
        assertNotEquals(plain, sealed);
        assertEquals(plain, encryptor.decrypt(sealed));
    }

    @Test
    void nullSafe() {
        assertNull(encryptor.encrypt(null));
        assertNull(encryptor.decrypt(null));
    }
}
