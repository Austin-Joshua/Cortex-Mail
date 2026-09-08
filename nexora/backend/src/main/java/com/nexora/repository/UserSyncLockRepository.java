package com.nexora.repository;

import com.nexora.model.UserSyncLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserSyncLockRepository extends JpaRepository<UserSyncLock, Long> {

    /**
     * Atomic claim: only one concurrent transaction can update a free/expired lock row.
     * @return 1 if this caller owns the lock, 0 if still held by someone else
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE UserSyncLock l
               SET l.lockOwner = :owner, l.lockedUntil = :until
             WHERE l.userId = :userId
               AND (l.lockedUntil IS NULL OR l.lockedUntil < :now)
            """)
    int claimIfFree(
            @Param("userId") Long userId,
            @Param("owner") String owner,
            @Param("until") LocalDateTime until,
            @Param("now") LocalDateTime now);
}
