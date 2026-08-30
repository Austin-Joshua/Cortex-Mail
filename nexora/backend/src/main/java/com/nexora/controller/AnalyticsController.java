package com.nexora.controller;

import com.nexora.security.UserPrincipal;
import com.nexora.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final EmailService emailService;

    @GetMapping("/volume")
    public ResponseEntity<List<Map<String, Object>>> getEmailVolume(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(emailService.getEmailVolume(user.getId(), days));
    }
}
