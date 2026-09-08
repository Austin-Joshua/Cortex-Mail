package com.nexora.repository;

import com.nexora.model.BackgroundJob;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BackgroundJobRepository extends JpaRepository<BackgroundJob, Long> {

    @Query("""
            SELECT j FROM BackgroundJob j
            WHERE j.status = :status AND j.runAt <= :now
            ORDER BY j.runAt ASC
            """)
    List<BackgroundJob> findDueJobs(@Param("status") String status,
                                    @Param("now") LocalDateTime now,
                                    Pageable pageable);

    /** Atomic claim — only one worker wins when status is still PENDING. */
    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE BackgroundJob j
               SET j.status = :running, j.lockedAt = :now, j.attempts = COALESCE(j.attempts, 0) + 1
             WHERE j.id = :id AND j.status = :pending
            """)
    int tryClaim(
            @Param("id") Long id,
            @Param("pending") String pending,
            @Param("running") String running,
            @Param("now") LocalDateTime now);
}
