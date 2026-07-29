package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.model.Email;
import com.nexora.model.User;
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
    public ResponseEntity<ApiResponse<List<Email>>> getPriorityEmails(
            @RequestParam(defaultValue = "20") int limit,
            @AuthenticationPrincipal User user) {
        List<Email> priorityEmails = emailService.getPriorityEmails(user.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(priorityEmails));
    }

    @PostMapping("/{emailId}/flag")
    public ResponseEntity<ApiResponse<Email>> flagAsImportant(
            @PathVariable Long emailId,
            @AuthenticationPrincipal User user) {
        Email email = emailService.flagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @PostMapping("/{emailId}/unflag")
    public ResponseEntity<ApiResponse<Email>> unflagAsImportant(
            @PathVariable Long emailId,
            @AuthenticationPrincipal User user) {
        Email email = emailService.unflagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Email>>> getPrioritySuggestions(@AuthenticationPrincipal User user) {
        List<Email> suggestions = emailService.getSuggestedPriorityEmails(user.getId());
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
