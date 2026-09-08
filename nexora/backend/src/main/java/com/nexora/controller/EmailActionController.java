package com.nexora.controller;

import com.nexora.dto.response.EmailActionResponse;
import com.nexora.model.EmailAction;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.UserPrincipal;
import com.nexora.repository.EmailActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/email-actions")
@RequiredArgsConstructor
public class EmailActionController {

    private final EmailActionRepository emailActionRepository;

    @GetMapping("/pending")
    public ResponseEntity<List<EmailActionResponse>> pending(
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = AuthPrincipals.requireId(user);
        LocalDateTime now = LocalDateTime.now();
        List<EmailActionResponse> items = emailActionRepository.findPendingOpenActions(userId, now)
                .stream()
                .map(EmailActionController::toResponse)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/{id}/snooze")
    public ResponseEntity<EmailActionResponse> snooze(
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int hours,
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = AuthPrincipals.requireId(user);
        int safeHours = Math.max(1, Math.min(hours, 24 * 14));
        return emailActionRepository.findByIdAndUserId(id, userId)
                .map(action -> {
                    action.setSnoozedUntil(LocalDateTime.now().plusHours(safeHours));
                    action.setIsCompleted(false);
                    return ResponseEntity.ok(toResponse(emailActionRepository.save(action)));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Void> completeAction(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        emailActionRepository.findByIdAndUserId(id, AuthPrincipals.requireId(user))
            .ifPresent(action -> {
                action.setIsCompleted(true);
                emailActionRepository.save(action);
            });
        return ResponseEntity.noContent().build();
    }

    private static EmailActionResponse toResponse(EmailAction action) {
        EmailActionResponse response = new EmailActionResponse();
        response.setId(action.getId());
        if (action.getEmail() != null) {
            response.setEmailId(action.getEmail().getId());
            response.setEmailSubject(action.getEmail().getSubject());
        }
        response.setActionType(action.getActionType() != null ? action.getActionType().name() : null);
        response.setActionDescription(action.getActionDescription());
        response.setDeadline(action.getDeadline() != null ? action.getDeadline().toString() : null);
        response.setIsCompleted(action.getIsCompleted());
        response.setSnoozedUntil(action.getSnoozedUntil() != null ? action.getSnoozedUntil().toString() : null);
        return response;
    }
}
