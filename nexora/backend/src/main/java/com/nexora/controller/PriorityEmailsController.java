package com.nexora.controller;

import com.nexora.dto.ApiResponse;
import com.nexora.model.Email;
import com.nexora.model.User;
import com.nexora.service.EmailService;
import com.nexora.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/priority")
@RequiredArgsConstructor
public class PriorityEmailsController {
    private final EmailService emailService;
    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Email>>> getPriorityEmails(
            @RequestParam(defaultValue = "20") int limit,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        List<Email> priorityEmails = emailService.getPriorityEmails(user.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(priorityEmails));
    }

    @PostMapping("/{emailId}/flag")
    public ResponseEntity<ApiResponse<Email>> flagAsImportant(
            @PathVariable Long emailId,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        Email email = emailService.flagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @PostMapping("/{emailId}/unflag")
    public ResponseEntity<ApiResponse<Email>> unflagAsImportant(
            @PathVariable Long emailId,
            Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        Email email = emailService.unflagAsImportant(user.getId(), emailId);
        return ResponseEntity.ok(ApiResponse.success(email));
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<Email>>> getPrioritySuggestions(Authentication auth) {
        User user = userService.findByEmail(auth.getName());
        List<Email> suggestions = emailService.getSuggestedPriorityEmails(user.getId());
        return ResponseEntity.ok(ApiResponse.success(suggestions));
    }
}
