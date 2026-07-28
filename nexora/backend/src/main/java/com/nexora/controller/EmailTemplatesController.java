package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.model.EmailTemplate;
import com.nexora.model.User;
import com.nexora.service.EmailTemplateService;
import com.nexora.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplatesController {
    private final EmailTemplateService templateService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getTemplates(Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        List<EmailTemplate> templates = templateService.getUserTemplates(user.getId());
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailTemplate>> createTemplate(
            @RequestBody EmailTemplate template,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        EmailTemplate created = templateService.createTemplate(user, template);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateTemplate(
            @PathVariable Long id,
            @RequestBody EmailTemplate template,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        EmailTemplate updated = templateService.updateTemplate(user, id, template);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        templateService.deleteTemplate(user, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
