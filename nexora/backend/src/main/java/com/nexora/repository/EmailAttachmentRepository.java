package com.nexora.repository;

import com.nexora.model.EmailAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailAttachmentRepository extends JpaRepository<EmailAttachment, Long> {
    List<EmailAttachment> findByEmailId(Long emailId);
    void deleteByEmailId(Long emailId);
}
