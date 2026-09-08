package com.nexora.service;

import com.nexora.model.UserSyncLock;
import com.nexora.repository.UserSyncLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSyncLockService {

    private static final int DEFAULT_TTL_MINUTES = 20;

    private final UserSyncLockRepository lockRepository;

    /**
     * Try to acquire a durable per-user sync lock. Returns lock owner token on success, null if held.
     */
    @Transactional
    public String tryLock(Long userId) {
        return tryLock(userId, DEFAULT_TTL_MINUTES);
    }

    @Transactional
    public String tryLock(Long userId, int ttlMinutes) {
        if (userId == null) {
            return null;
        }
        ensureRow(userId);
        LocalDateTime now = LocalDateTime.now();
        String owner = UUID.randomUUID().toString();
        LocalDateTime until = now.plusMinutes(Math.max(1, ttlMinutes));
        int claimed = lockRepository.claimIfFree(userId, owner, until, now);
        return claimed == 1 ? owner : null;
    }

    private void ensureRow(Long userId) {
        if (lockRepository.existsById(userId)) {
            return;
        }
        try {
            lockRepository.saveAndFlush(UserSyncLock.builder().userId(userId).build());
        } catch (DataIntegrityViolationException ignored) {
            // Concurrent first-lock insert — row now exists for the claim UPDATE.
        }
    }

    @Transactional
    public void unlock(Long userId, String owner) {
        if (userId == null) {
            return;
        }
        lockRepository.findById(userId).ifPresent(lock -> {
            if (owner == null || owner.equals(lock.getLockOwner())) {
                lock.setLockedUntil(null);
                lock.setLockOwner(null);
                lockRepository.save(lock);
            }
        });
    }

    @Transactional(readOnly = true)
    public boolean isLocked(Long userId) {
        if (userId == null) {
            return false;
        }
        return lockRepository.findById(userId)
                .map(lock -> lock.getLockedUntil() != null && lock.getLockedUntil().isAfter(LocalDateTime.now()))
                .orElse(false);
    }
}
