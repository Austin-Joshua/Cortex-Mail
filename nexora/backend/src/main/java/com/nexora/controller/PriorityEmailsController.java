package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.dto.response.EmailResponse;
import com.nexora.security.UserPrincipal;
import com.nexora.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/priority")
@RequiredArgsConstructor
public class PriorityEmailsController {
    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getPriorityEmails(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal UserPrincipal user) {
        List<EmailResponse> priorityEmails = emailService.getPriorityEmails(user.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(priorityEmails));
    }

    @PostMapping("/{emailId}/flag")
    public ResponseEntity<ApiResponse<EmailResponse>> flagAsImportant(
            @PathVariable Long emailId,
            @AuthenticationPrincipal UserPrincipal user) {
        EmailResponse email = emailService.flagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @PostMapping("/{emailId}/unflag")
    public ResponseEntity<ApiResponse<EmailResponse>> unflagAsImportant(
            @PathVariable Long emailId,
            @AuthenticationPrincipal UserPrincipal user) {
        EmailResponse email = emailService.unflagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<EmailResponse>>> getPrioritySuggestions(
            @AuthenticationPrincipal UserPrincipal user) {
        List<EmailResponse> suggestions = emailService.getSuggestedPriorityEmails(user.getId());
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
