package com.nexora.repository;

import com.nexora.model.EmailDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailDraftRepository extends JpaRepository<EmailDraft, Long> {
    List<EmailDraft> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<EmailDraft> findByIdAndUserId(Long id, Long userId);
}
