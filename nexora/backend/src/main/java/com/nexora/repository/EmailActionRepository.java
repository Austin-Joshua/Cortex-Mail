package com.nexora.repository;

import com.nexora.model.EmailAction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
