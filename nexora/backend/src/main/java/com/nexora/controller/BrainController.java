package com.nexora.controller;

import com.nexora.dto.request.BrainQueryRequest;
import com.nexora.dto.response.BrainConversationResponse;
import com.nexora.dto.response.BrainQueryResponse;
import com.nexora.security.AuthPrincipals;
import com.nexora.security.UserPrincipal;
import com.nexora.service.NexoraBrainService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/brain")
public class BrainController {

    private final NexoraBrainService brainService;

    public BrainController(NexoraBrainService brainService) {
        this.brainService = brainService;
    }

    @PostMapping("/query")
    @RateLimiter(name = "brain-query")
    public ResponseEntity<BrainQueryResponse> query(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestBody @Valid BrainQueryRequest request) {
        Long userId = AuthPrincipals.requireId(user);
        BrainQueryResponse response = brainService.query(userId, request.getQuery());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<BrainConversationResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal user) {
        Long userId = AuthPrincipals.requireId(user);
        List<BrainConversationResponse> history = brainService.getHistory(userId);
        return ResponseEntity.ok(history);
    }
}
