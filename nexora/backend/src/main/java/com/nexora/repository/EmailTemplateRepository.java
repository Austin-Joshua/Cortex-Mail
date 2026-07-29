package com.nexora.repository;

import com.nexora.model.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findByUserIdOrderByUsageCountDesc(Long userId);
    Optional<EmailTemplate> findByIdAndUserId(Long id, Long userId);
}
