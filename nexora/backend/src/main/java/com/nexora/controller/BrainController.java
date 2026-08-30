package com.nexora.controller;

import com.nexora.dto.request.BrainQueryRequest;
import com.nexora.dto.response.BrainConversationResponse;
import com.nexora.dto.response.BrainQueryResponse;
import com.nexora.exception.NexoraException;
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
    @RateLimiter(name = "brain-query", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<BrainQueryResponse> query(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody BrainQueryRequest request) {
        requireUser(user);
        return ResponseEntity.ok(brainService.query(user.getId(), request.getQuery()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<BrainConversationResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal user) {
        requireUser(user);
        return ResponseEntity.ok(brainService.getHistory(user.getId()));
    }

    public ResponseEntity<BrainQueryResponse> rateLimitFallback(
            UserPrincipal user,
            BrainQueryRequest request,
            Exception ex) {
        return ResponseEntity.status(429).body(
                BrainQueryResponse.builder()
                        .answer("You've reached the query limit (20 queries/hour). Please try again later.")
                        .build());
    }

    private static void requireUser(UserPrincipal user) {
        if (user == null || user.getId() == null) {
            throw new NexoraException("Unauthorized", 401);
        }
    }
}
