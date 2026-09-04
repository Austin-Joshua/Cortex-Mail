package com.nexora.controller;

import com.nexora.security.AuthPrincipals;
import com.nexora.security.UserPrincipal;
import com.nexora.repository.EmailActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/email-actions")
@RequiredArgsConstructor
public class EmailActionController {

    private final EmailActionRepository emailActionRepository;

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
}
