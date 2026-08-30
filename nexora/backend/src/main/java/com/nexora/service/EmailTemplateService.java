package com.nexora.service;

import com.nexora.dto.request.TemplateRequest;
import com.nexora.dto.response.TemplateResponse;
import com.nexora.model.EmailTemplate;
import com.nexora.model.User;
import com.nexora.repository.EmailTemplateRepository;
import com.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<TemplateResponse> getUserTemplates(Long userId) {
        ensureUser(userId);
        List<EmailTemplate> templates = templateRepository.findByUserIdOrderByUsageCountDesc(userId);
        List<TemplateResponse> result = new ArrayList<>(templates.size());
        for (EmailTemplate template : templates) {
            result.add(toDto(template));
        }
        return result;
    }

    @Transactional
    public TemplateResponse createTemplate(Long userId, TemplateRequest request) {
        ensureUser(userId);
        if (request == null || request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template name is required");
        }

        User user = userRepository.getReferenceById(userId);
        EmailTemplate template = new EmailTemplate();
        template.setUser(user);
        template.setName(request.getName().trim());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        template.setHtmlBody(request.getHtmlBody());
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory().trim());
        }
        template.setUsageCount(0);

        EmailTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    @Transactional
    public TemplateResponse updateTemplate(Long userId, Long templateId, TemplateRequest request) {
        ensureUser(userId);
        ensureTemplateId(templateId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template body is required");
        }
        if (request.getName() != null && request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template name cannot be blank");
        }

        EmailTemplate existing = findOwned(userId, templateId);
        if (request.getName() != null) {
            existing.setName(request.getName().trim());
        }
        if (request.getSubject() != null) {
            existing.setSubject(request.getSubject());
        }
        if (request.getBody() != null) {
            existing.setBody(request.getBody());
        }
        if (request.getHtmlBody() != null) {
            existing.setHtmlBody(request.getHtmlBody());
        }
        if (request.getCategory() != null) {
            existing.setCategory(request.getCategory().trim());
        }

        EmailTemplate saved = templateRepository.save(existing);
        return toDto(saved);
    }

    @Transactional
    public void deleteTemplate(Long userId, Long templateId) {
        ensureUser(userId);
        ensureTemplateId(templateId);
        templateRepository.delete(findOwned(userId, templateId));
    }

    @Transactional
    public TemplateResponse recordUsage(Long userId, Long templateId) {
        ensureUser(userId);
        ensureTemplateId(templateId);

        EmailTemplate template = findOwned(userId, templateId);
        int current = template.getUsageCount() != null ? template.getUsageCount() : 0;
        template.setUsageCount(current + 1);

        EmailTemplate saved = templateRepository.save(template);
        return toDto(saved);
    }

    private EmailTemplate findOwned(Long userId, Long templateId) {
        return templateRepository.findByIdAndUserId(templateId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Template " + templateId + " not found"));
    }

    private static void ensureUser(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static void ensureTemplateId(Long templateId) {
        if (templateId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Template id is required");
        }
    }

    private static TemplateResponse toDto(EmailTemplate entity) {
        TemplateResponse dto = new TemplateResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setHtmlBody(entity.getHtmlBody());
        dto.setCategory(entity.getCategory());
        dto.setUsageCount(entity.getUsageCount() != null ? entity.getUsageCount() : 0);
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
