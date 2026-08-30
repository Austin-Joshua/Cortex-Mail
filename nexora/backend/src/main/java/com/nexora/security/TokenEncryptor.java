package com.nexora.security;

import lombok.extern.slf4j.Slf4j;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Encrypts Gmail tokens at rest with AES-256-GCM.
 * Legacy AES/CBC payloads (no version prefix) still decrypt for migration.
 */
@Component
@Slf4j
public class TokenEncryptor {

    private static final String GCM = "AES/GCM/NoPadding";
    private static final String CBC = "AES/CBC/PKCS5PADDING";
    private static final byte VERSION_GCM = 1;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.encryption-key}")
    private String encryptionKey;

    @PostConstruct
    void validateKey() {
        if (encryptionKey == null) {
            throw new IllegalStateException("app.encryption-key is required");
        }
        int length = encryptionKey.getBytes(StandardCharsets.UTF_8).length;
        if (length != 16 && length != 24 && length != 32) {
            throw new IllegalStateException("app.encryption-key must be 16, 24, or 32 bytes");
        }
    }

    public String encrypt(String plainText) {
        if (plainText == null) return null;
        try {
            SecretKey secretKey = secretKey();
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(GCM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(1 + GCM_IV_LENGTH + encrypted.length);
            buffer.put(VERSION_GCM);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("Encryption failed", e);
            throw new RuntimeException("Token encryption failed", e);
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null) return null;
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            if (decoded.length < 17) {
                throw new IllegalArgumentException("Invalid encrypted text: too short");
            }
            if (decoded[0] == VERSION_GCM) {
                return decryptGcm(decoded);
            }
            return decryptLegacyCbc(decoded);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Decryption failed", e);
            throw new RuntimeException("Token decryption failed", e);
        }
    }

    private String decryptGcm(byte[] decoded) throws Exception {
        byte[] iv = new byte[GCM_IV_LENGTH];
        System.arraycopy(decoded, 1, iv, 0, GCM_IV_LENGTH);
        byte[] ciphertext = new byte[decoded.length - 1 - GCM_IV_LENGTH];
        System.arraycopy(decoded, 1 + GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(GCM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), new GCMParameterSpec(GCM_TAG_BITS, iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    /** Pre-GCM format: 16-byte IV + CBC ciphertext. */
    private String decryptLegacyCbc(byte[] decoded) throws Exception {
        byte[] iv = new byte[16];
        System.arraycopy(decoded, 0, iv, 0, 16);
        byte[] ciphertext = new byte[decoded.length - 16];
        System.arraycopy(decoded, 16, ciphertext, 0, ciphertext.length);

        Cipher cipher = Cipher.getInstance(CBC);
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), new IvParameterSpec(iv));
        return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
    }

    private SecretKey secretKey() {
        byte[] keyBytes = encryptionKey.getBytes(StandardCharsets.UTF_8);
        return new SecretKeySpec(keyBytes, "AES");
    }
}
