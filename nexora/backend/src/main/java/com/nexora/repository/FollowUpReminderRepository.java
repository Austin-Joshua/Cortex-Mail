package com.nexora.repository;

import com.nexora.model.FollowUpReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FollowUpReminderRepository extends JpaRepository<FollowUpReminder, Long> {
    List<FollowUpReminder> findByUser_IdAndStatus(Long userId, String status);

    Optional<FollowUpReminder> findFirstByUser_IdAndEmail_IdAndStatus(Long userId, Long emailId, String status);
}
