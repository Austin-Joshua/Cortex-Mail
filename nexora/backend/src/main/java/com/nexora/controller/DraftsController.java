package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.model.EmailDraft;
import com.nexora.model.User;
import com.nexora.service.EmailDraftService;
import com.nexora.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftsController {
    private final EmailDraftService draftService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailDraft>>> getDrafts(Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        List<EmailDraft> drafts = draftService.getUserDrafts(user.getId());
        return ResponseEntity.ok(ApiResponse.success(drafts));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailDraft>> createDraft(
            @RequestBody EmailDraft draft,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        EmailDraft created = draftService.createDraft(user, draft);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailDraft>> updateDraft(
            @PathVariable Long id,
            @RequestBody EmailDraft draft,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        EmailDraft updated = draftService.updateDraft(user, id, draft);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable Long id,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        draftService.deleteDraft(user, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<String>> sendDraft(
            @PathVariable Long id,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        String messageId = draftService.sendDraft(user, id);
        return ResponseEntity.ok(ApiResponse.success(messageId));
    }
}
