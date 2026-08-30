package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.dto.request.DraftRequest;
import com.nexora.dto.response.DraftResponse;
import com.nexora.security.UserPrincipal;
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
    public ResponseEntity<ApiResponse<List<DraftResponse>>> getDrafts(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(draftService.getUserDrafts(user.getId())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DraftResponse>> createDraft(
            @RequestBody DraftRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(draftService.createDraft(user.getId(), request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DraftResponse>> updateDraft(
            @PathVariable Long id,
            @RequestBody DraftRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(
                draftService.updateDraft(user.getId(), id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        draftService.deleteDraft(user.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<String>> sendDraft(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.success(draftService.sendDraft(user.getId(), id)));
    }
}
