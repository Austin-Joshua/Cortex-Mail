package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.model.EmailDraft;
import com.nexora.model.User;
import com.nexora.service.EmailDraftService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftsController {
    private final EmailDraftService draftService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailDraft>>> getDrafts(@AuthenticationPrincipal User user) {
        List<EmailDraft> drafts = draftService.getUserDrafts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(drafts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailDraft>> createDraft(
            @RequestBody EmailDraft draft,
            @AuthenticationPrincipal User user) {
        EmailDraft created = draftService.createDraft(user, draft);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailDraft>> updateDraft(
            @PathVariable Long id,
            @RequestBody EmailDraft draft,
            @AuthenticationPrincipal User user) {
        EmailDraft updated = draftService.updateDraft(user, id, draft);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        draftService.deleteDraft(user, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<String>> sendDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        String messageId = draftService.sendDraft(user, id);
        return ResponseEntity.ok(ApiResponse.success(messageId));
    }
}
