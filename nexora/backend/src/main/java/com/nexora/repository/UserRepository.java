package com.nexora.repository;

import com.nexora.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleId(String googleId);
    Optional<User> findByEmail(String email);
    boolean existsByGoogleId(String googleId);
    List<User> findAllByLastSyncedAtBeforeOrLastSyncedAtIsNull(LocalDateTime threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.gmailAccessToken = :token, u.tokenExpiry = :expiry WHERE u.id = :id")
    int updateAccessToken(@Param("id") Long id,
                          @Param("token") String token,
                          @Param("expiry") LocalDateTime expiry);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.gmailLabelCounts = :counts WHERE u.id = :id")
    int updateLabelCounts(@Param("id") Long id, @Param("counts") String counts);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.gmailHistoryId = :historyId WHERE u.id = :id")
    int updateHistoryId(@Param("id") Long id, @Param("historyId") String historyId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.lastSyncedAt = :syncedAt WHERE u.id = :id")
    int updateLastSyncedAt(@Param("id") Long id, @Param("syncedAt") LocalDateTime syncedAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE User u SET u.gmailHistoryId = :historyId, u.lastSyncedAt = :syncedAt WHERE u.id = :id")
    int updateSyncCheckpoint(@Param("id") Long id,
                             @Param("historyId") String historyId,
                             @Param("syncedAt") LocalDateTime syncedAt);
}
