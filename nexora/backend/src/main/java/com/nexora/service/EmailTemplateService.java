package com.nexora.service;

import com.nexora.model.EmailTemplate;
import com.nexora.model.User;
import com.nexora.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository templateRepository;

    public List<EmailTemplate> getUserTemplates(Long userId) {
        return templateRepository.findByUserIdOrderByUsageCountDesc(userId);
    }

    @Transactional
    public EmailTemplate createTemplate(User user, EmailTemplate template) {
        template.setId(null);
        template.setUser(user);
        if (template.getUsageCount() == null) {
            template.setUsageCount(0);
        }
        return templateRepository.save(template);
    }

    @Transactional
    public EmailTemplate updateTemplate(User user, Long id, EmailTemplate incoming) {
        EmailTemplate existing = owned(user, id);
        existing.setName(incoming.getName());
        existing.setSubject(incoming.getSubject());
        existing.setBody(incoming.getBody());
        existing.setHtmlBody(incoming.getHtmlBody());
        existing.setCategory(incoming.getCategory());
        return templateRepository.save(existing);
    }

    @Transactional
    public void deleteTemplate(User user, Long id) {
        templateRepository.delete(owned(user, id));
    }

    private EmailTemplate owned(User user, Long id) {
        return templateRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Template " + id + " not found"));
    }
}
