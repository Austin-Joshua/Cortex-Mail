package com.nexora.repository;

import com.nexora.model.EmailAction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailActionRepository extends JpaRepository<EmailAction, Long> {
    @EntityGraph(attributePaths = "email")
    List<EmailAction> findByUserIdAndIsCompletedFalseOrderByDeadlineAsc(Long userId);

    @EntityGraph(attributePaths = "email")
    List<EmailAction> findByUserIdAndDeadlineBetweenOrderByDeadlineAsc(
        Long userId, LocalDateTime start, LocalDateTime end);
    List<EmailAction> findByEmailId(Long emailId);
    List<EmailAction> findByEmailIdAndUserId(Long emailId, Long userId);
    Optional<EmailAction> findByIdAndUserId(Long id, Long userId);

    long countByUserIdAndIsCompletedFalse(Long userId);

    @Query("""
            SELECT COUNT(a) FROM EmailAction a
            WHERE a.userId = :userId AND a.isCompleted = false
              AND a.email.inInbox = true
              AND (a.email.isTrash = false OR a.email.isTrash IS NULL)
              AND (a.email.isSpam = false OR a.email.isSpam IS NULL)
              AND (a.email.isDraft = false OR a.email.isDraft IS NULL)
              AND (a.snoozedUntil IS NULL OR a.snoozedUntil <= :now)
            """)
    long countOpenInboxFollowUps(@Param("userId") Long userId, @Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = "email")
    List<EmailAction> findTop8ByUserIdAndIsCompletedFalseOrderByDeadlineAsc(Long userId);

    @EntityGraph(attributePaths = "email")
    @Query("""
            SELECT a FROM EmailAction a
            WHERE a.userId = :userId AND a.isCompleted = false
              AND a.email.inInbox = true
              AND (a.email.isTrash = false OR a.email.isTrash IS NULL)
              AND (a.email.isSpam = false OR a.email.isSpam IS NULL)
              AND (a.email.isDraft = false OR a.email.isDraft IS NULL)
              AND (a.snoozedUntil IS NULL OR a.snoozedUntil <= :now)
            ORDER BY a.deadline ASC
            """)
    List<EmailAction> findOpenInboxFollowUps(@Param("userId") Long userId,
                                               @Param("now") LocalDateTime now,
                                               Pageable pageable);

    @EntityGraph(attributePaths = "email")
    @Query("""
            SELECT a FROM EmailAction a
            WHERE a.userId = :userId AND a.isCompleted = false
              AND (a.snoozedUntil IS NULL OR a.snoozedUntil <= :now)
            ORDER BY a.deadline ASC NULLS LAST, a.createdAt DESC
            """)
    List<EmailAction> findPendingOpenActions(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
