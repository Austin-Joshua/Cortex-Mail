package com.nexora.repository;

import com.nexora.model.Email;
import com.nexora.model.Email.EmailCategory;
import com.nexora.model.Email.Priority;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {

    boolean existsByUserIdAndGmailMessageId(Long userId, String gmailMessageId);

    @EntityGraph(attributePaths = "attachments")
    Optional<Email> findByUserIdAndGmailMessageId(Long userId, String gmailMessageId);

    @EntityGraph(attributePaths = "attachments")
    List<Email> findByUserIdAndGmailMessageIdIn(Long userId, Collection<String> gmailMessageIds);

    @EntityGraph(attributePaths = {"attachments", "actions"})
    @Query("SELECT e FROM Email e WHERE e.id = :id AND e.user.id = :userId")
    Optional<Email> findDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /** Lightweight ownership lookup for mutations — no attachment/action graph. */
    @Query("SELECT e FROM Email e WHERE e.id = :id AND e.user.id = :userId")
    Optional<Email> findOwnedByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    Page<Email> findByUserIdOrderByReceivedAtDesc(Long userId, Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueOrderByReceivedAtDesc(Long userId, Pageable pageable);

    /**
     * Archive local inbox rows that are no longer in Gmail's INBOX listing.
     * Bulk UPDATE avoids loading body TEXT columns into the persistence context.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Email e SET e.inInbox = false, e.isArchived = true
            WHERE e.user.id = :userId AND e.inInbox = true
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.gmailMessageId NOT IN :gmailMessageIds
            """)
    int archiveInboxMissingFromGmail(
            @Param("userId") Long userId,
            @Param("gmailMessageIds") Collection<String> gmailMessageIds);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Email e SET e.inInbox = false, e.isArchived = true
            WHERE e.user.id = :userId AND e.inInbox IS NULL
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.gmailMessageId NOT IN :gmailMessageIds
            """)
    int archiveLegacyNullInboxMissingFromGmail(
            @Param("userId") Long userId,
            @Param("gmailMessageIds") Collection<String> gmailMessageIds);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Email e SET e.inInbox = true, e.isArchived = false, e.isDraft = false
            WHERE e.user.id = :userId AND e.inInbox IS NULL
              AND e.gmailMessageId IN :gmailMessageIds
            """)
    int restoreLegacyNullInboxPresentInGmail(
            @Param("userId") Long userId,
            @Param("gmailMessageIds") Collection<String> gmailMessageIds);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM Email e WHERE e.user.id = :userId AND e.gmailMessageId IN :gmailMessageIds")
    int deleteByUserIdAndGmailMessageIdIn(
            @Param("userId") Long userId,
            @Param("gmailMessageIds") Collection<String> gmailMessageIds);

    @Query("SELECT COUNT(e) FROM Email e WHERE e.user.id = :userId AND e.inInbox = true AND e.isRead = false")
    long countInboxUnreadByUserId(@Param("userId") Long userId);

    long countByUserIdAndInInboxTrue(Long userId);

    long countByUserIdAndIsDraftTrue(Long userId);

    long countByUserIdAndIsArchivedTrue(Long userId);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(e) FROM Email e WHERE e.user.id = :userId AND e.category = :category AND e.inInbox = true")
    long countByUserIdAndCategoryAndInInboxTrue(@Param("userId") Long userId, @Param("category") EmailCategory category);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Email> searchInboxByUserId(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true AND e.category = :category AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Email> searchInboxByUserIdAndCategory(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("category") EmailCategory category,
            Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueAndCategoryOrderByReceivedAtDesc(
            Long userId, EmailCategory category, Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueAndPriorityOrderByReceivedAtDesc(
            Long userId, Priority priority, Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueAndCategoryAndPriorityOrderByReceivedAtDesc(
            Long userId, EmailCategory category, Priority priority, Pageable pageable);

    Page<Email> findByUserIdAndIsDraftTrueOrderByReceivedAtDesc(Long userId, Pageable pageable);

    Page<Email> findByUserIdAndIsArchivedTrueOrderByReceivedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.isDraft = true AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.recipientTo) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Email> searchDraftsByUserId(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND e.isArchived = true AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Email> searchArchivedByUserId(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    Page<Email> findByUserIdAndCategoryOrderByReceivedAtDesc(Long userId, EmailCategory category, Pageable pageable);

    Page<Email> findByUserIdAndPriorityOrderByReceivedAtDesc(Long userId, Priority priority, Pageable pageable);

    Page<Email> findByUserIdAndCategoryAndPriorityOrderByReceivedAtDesc(
        Long userId, EmailCategory category, Priority priority, Pageable pageable);

    @Query("SELECT e FROM Email e WHERE e.user.id = :userId AND " +
           "(LOWER(e.subject) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(e.senderEmail) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Email> searchByUserId(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    @Query("SELECT e.category, COUNT(e) FROM Email e WHERE e.user.id = :userId AND e.inInbox = true GROUP BY e.category")
    List<Object[]> countByUserIdGroupByCategory(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Email e SET e.category = 'UNCATEGORIZED' WHERE e.user.id = :userId AND e.inInbox = true AND e.category <> 'SPAM'")
    int resetInboxCategoriesExceptSpam(@Param("userId") Long userId);

    List<Email> findByUserIdAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
        Long userId, Priority priority, Pageable pageable);

    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND (e.isTrash = false OR e.isTrash IS NULL)
              AND (e.isSpam = false OR e.isSpam IS NULL)
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.category <> 'PROMOTIONAL' AND e.category <> 'SPAM'
              AND e.deadlineDetected BETWEEN :start AND :end
            ORDER BY e.deadlineDetected ASC
            """)
    List<Email> findUpcomingDeadlines(@Param("userId") Long userId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(e) FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND (e.isTrash = false OR e.isTrash IS NULL)
              AND (e.isSpam = false OR e.isSpam IS NULL)
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.category <> 'PROMOTIONAL' AND e.category <> 'SPAM'
              AND e.deadlineDetected >= :since AND e.deadlineDetected < :now
            """)
    long countOverdueDeadlines(@Param("userId") Long userId,
                               @Param("now") LocalDateTime now,
                               @Param("since") LocalDateTime since);

    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND (e.isTrash = false OR e.isTrash IS NULL)
              AND (e.isSpam = false OR e.isSpam IS NULL)
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.category = 'MEETING'
              AND e.deadlineDetected BETWEEN :start AND :end
            ORDER BY e.deadlineDetected ASC
            """)
    List<Email> findTodaysMeetings(@Param("userId") Long userId,
                                   @Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);

    @Query("""
            SELECT COUNT(e) FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND (e.isTrash = false OR e.isTrash IS NULL)
              AND (e.isSpam = false OR e.isSpam IS NULL)
              AND (e.isDraft = false OR e.isDraft IS NULL)
              AND e.category = 'MEETING'
              AND e.deadlineDetected BETWEEN :start AND :end
            """)
    long countTodaysMeetings(@Param("userId") Long userId,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end);

    List<Email> findTop80ByUserIdOrderByReceivedAtDesc(Long userId);

    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId
              AND e.isTrash = false AND e.isSpam = false AND e.isDraft = false
              AND (
                (e.recipientCc IS NOT NULL AND e.recipientCc <> '')
                OR UPPER(COALESCE(e.gmailLabelIds, '')) LIKE '%CATEGORY_FORUMS%'
                OR UPPER(COALESCE(e.gmailLabelIds, '')) LIKE '%CATEGORY_SOCIAL%'
              )
            ORDER BY e.receivedAt DESC
            """)
    Page<Email> findSharedMailbox(@Param("userId") Long userId, Pageable pageable);

    /** Important / starred / high-priority inbox mail — candidates for selective Gemini enrichment. */
    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true AND e.isDraft = false
            AND (e.isImportant = true OR e.isStarred = true OR e.priority = 'HIGH')
            ORDER BY e.receivedAt DESC
            """)
    Page<Email> findGeminiPriorityCandidates(@Param("userId") Long userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(Long userId);

    /**
     * Grouped sender stats with the subject of the newest message (not lexicographic MAX).
     */
    @Query(value = """
        SELECT sender_email,
               MAX(sender_name) AS sender_name,
               COUNT(*) AS email_count,
               MAX(received_at) AS latest_received_at,
               (ARRAY_AGG(subject ORDER BY received_at DESC NULLS LAST))[1] AS latest_subject
        FROM emails
        WHERE user_id = :userId
        GROUP BY sender_email
        ORDER BY email_count DESC
        """, nativeQuery = true)
    List<Object[]> countBySenderForUser(@Param("userId") Long userId);

    List<Email> findByUserIdAndInInboxTrueAndPriorityAndIsReadFalseOrderByReceivedAtDesc(
            Long userId, Priority priority, Pageable pageable);

    /**
     * Fetch all email received dates for a user after a start date.
     */
    @Query("SELECT e.receivedAt FROM Email e WHERE e.user.id = :userId AND e.receivedAt >= :start")
    List<LocalDateTime> findReceivedAtByUserIdAndReceivedAtAfter(@Param("userId") Long userId, @Param("start") LocalDateTime start);

    /**
     * Fetch all emails in a thread for a given user, ordered by received date ASC.
     */
    List<Email> findByUserIdAndGmailThreadIdOrderByReceivedAtAsc(Long userId, String gmailThreadId);

    /**
     * Fetch all emails from a specific sender for a given user, newest first.
     */
    Page<Email> findByUserIdAndSenderEmailOrderByReceivedAtDesc(
            Long userId, String senderEmail, Pageable pageable);

    List<Email> findByUserIdAndIdIn(Long userId, Collection<Long> ids);

    Page<Email> findByUserIdAndInInboxTrueAndIsReadFalseOrderByReceivedAtDesc(Long userId, Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueAndIsStarredTrueOrderByReceivedAtDesc(Long userId, Pageable pageable);

    Page<Email> findByUserIdAndInInboxTrueAndIsImportantTrueOrderByReceivedAtDesc(Long userId, Pageable pageable);

    @Query("""
            SELECT e.id, e.gmailMessageId FROM Email e
            WHERE e.user.id = :userId AND e.inInbox = true AND e.isRead = false
            ORDER BY e.receivedAt DESC
            """)
    List<Object[]> findUnreadInboxIds(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND UPPER(COALESCE(e.gmailLabelIds, '')) LIKE CONCAT('%', :label, '%')
            ORDER BY e.receivedAt DESC
            """)
    Page<Email> findInboxByGmailLabel(
            @Param("userId") Long userId, @Param("label") String label, Pageable pageable);

    @Query("""
            SELECT e FROM Email e WHERE e.user.id = :userId AND e.inInbox = true
              AND (
                UPPER(COALESCE(e.gmailLabelIds, '')) LIKE '%CATEGORY_PERSONAL%'
                OR (
                  UPPER(COALESCE(e.gmailLabelIds, '')) NOT LIKE '%CATEGORY_PROMOTIONS%'
                  AND UPPER(COALESCE(e.gmailLabelIds, '')) NOT LIKE '%CATEGORY_SOCIAL%'
                  AND UPPER(COALESCE(e.gmailLabelIds, '')) NOT LIKE '%CATEGORY_UPDATES%'
                  AND UPPER(COALESCE(e.gmailLabelIds, '')) NOT LIKE '%CATEGORY_FORUMS%'
                )
              )
            ORDER BY e.receivedAt DESC
            """)
    Page<Email> findInboxPrimary(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT LOWER(e.senderEmail), e.category, COUNT(e)
            FROM Email e
            WHERE e.user.id = :userId
              AND e.senderEmail IS NOT NULL
              AND e.category IS NOT NULL
              AND e.category <> 'UNCATEGORIZED'
              AND e.category <> 'SPAM'
            GROUP BY LOWER(e.senderEmail), e.category
            """)
    List<Object[]> countCategoriesBySender(@Param("userId") Long userId);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Email e SET e.isRead = true WHERE e.user.id = :userId AND e.id IN :ids")
    int markReadByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Email e SET e.isRead = false WHERE e.user.id = :userId AND e.id IN :ids")
    int markUnreadByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Email e SET e.isStarred = true WHERE e.user.id = :userId AND e.id IN :ids")
    int starByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Email e SET e.isStarred = false WHERE e.user.id = :userId AND e.id IN :ids")
    int unstarByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Email e SET e.inInbox = false, e.isArchived = true
            WHERE e.user.id = :userId AND e.id IN :ids
            """)
    int archiveByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);

    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Email e SET e.isTrash = true, e.inInbox = false, e.isArchived = false
            WHERE e.user.id = :userId AND e.id IN :ids
            """)
    int trashByUserIdAndIdIn(@Param("userId") Long userId, @Param("ids") Collection<Long> ids);
}

