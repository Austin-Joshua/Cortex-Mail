package com.nexora.service;

import com.nexora.model.UserSyncLock;
import com.nexora.repository.UserSyncLockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        LocalDateTime now = LocalDateTime.now();
        UserSyncLock existing = lockRepository.findById(userId).orElse(null);
        if (existing != null
                && existing.getLockedUntil() != null
                && existing.getLockedUntil().isAfter(now)) {
            return null;
        }
        String owner = UUID.randomUUID().toString();
        UserSyncLock lock = existing != null ? existing : UserSyncLock.builder().userId(userId).build();
        lock.setLockOwner(owner);
        lock.setLockedUntil(now.plusMinutes(Math.max(1, ttlMinutes)));
        lockRepository.save(lock);
        return owner;
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
