package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.dto.request.TemplateRequest;
import com.nexora.dto.response.TemplateResponse;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.UserPrincipal;
import com.nexora.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplatesController {

    private final EmailTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getTemplates(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getUserTemplates(AuthPrincipals.requireId(user))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(templateService.createTemplate(AuthPrincipals.requireId(user), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @PathVariable Long id,
            @RequestBody TemplateRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                templateService.updateTemplate(AuthPrincipals.requireId(user), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        templateService.deleteTemplate(AuthPrincipals.requireId(user), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/use")
    public ResponseEntity<ApiResponse<TemplateResponse>> recordUsage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                templateService.recordUsage(AuthPrincipals.requireId(user), id)));
    }
}
