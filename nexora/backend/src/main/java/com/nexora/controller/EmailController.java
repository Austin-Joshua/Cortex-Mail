package com.nexora.controller;

import com.nexora.dto.response.EmailResponse;
import com.nexora.dto.response.GmailLabelCountResponse;
import com.nexora.dto.response.GmailSyncResponse;
import com.nexora.dto.response.SenderSummaryResponse;
import com.nexora.dto.response.SyncIntegrityResponse;
import com.nexora.model.User;
import com.nexora.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @GetMapping
    public ResponseEntity<Page<EmailResponse>> getEmails(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(emailService.getEmails(user.getId(), category, priority, search, page, size));
    }

    @GetMapping("/drafts")
    public ResponseEntity<Page<EmailResponse>> getDraftEmails(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(emailService.getDraftEmails(user.getId(), search, page, size));
    }

    @GetMapping("/archived")
    public ResponseEntity<Page<EmailResponse>> getArchivedEmails(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(emailService.getArchivedEmails(user.getId(), search, page, size));
    }

    @GetMapping("/sync-status")
    public ResponseEntity<SyncIntegrityResponse> getSyncStatus(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.getSyncIntegrity(user));
    }

    @PostMapping("/sync")
    public ResponseEntity<GmailSyncResponse> syncEmails(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.syncInbox(user.getId()));
    }

    @GetMapping("/labels/counts")
    public ResponseEntity<Map<String, GmailLabelCountResponse>> getGmailLabelCounts(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.getGmailLabelCounts(user.getId()));
    }

    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classifyInbox(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.classifyInbox(user.getId(), user));
    }

    @GetMapping("/categories")
    public ResponseEntity<Map<String, Long>> getCategoryCounts(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.getCategoryCounts(user.getId()));
    }

    @GetMapping("/by-sender")
    public ResponseEntity<List<SenderSummaryResponse>> getEmailsBySender(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(emailService.getSenderSummary(user.getId()));
    }

    @GetMapping("/sender/{senderEmail}")
    public ResponseEntity<Page<EmailResponse>> getEmailsFromSender(
            @AuthenticationPrincipal User user,
            @PathVariable String senderEmail,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                emailService.getEmailsBySender(user.getId(), senderEmail, page, size));
    }

    @GetMapping("/thread/{threadId}")
    public ResponseEntity<List<EmailResponse>> getEmailThread(
            @AuthenticationPrincipal User user,
            @PathVariable String threadId) {
        return ResponseEntity.ok(emailService.getEmailThread(user.getId(), threadId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponse> getEmail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(emailService.getEmailDetail(user.getId(), id));
    }

    @PostMapping("/{id}/classify")
    public ResponseEntity<Map<String, String>> classifyEmail(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        emailService.classifyEmail(user.getId(), id, user);
        return ResponseEntity.ok(Map.of("message", "Classification started"));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markRead(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        emailService.markRead(user.getId(), id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/reaction")
    public ResponseEntity<Void> updateReaction(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reaction = body.getOrDefault("reaction", "NONE");
        emailService.updateReaction(user.getId(), id, reaction);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/draft-reply")
    public ResponseEntity<Map<String, String>> draftReply(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String style = body.getOrDefault("style", "PROFESSIONAL");
        String draft = emailService.draftReply(user.getId(), id, style);
        return ResponseEntity.ok(Map.of("draft", draft));
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<Map<String, String>> sendReply(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String replyBody = body.getOrDefault("replyBody", "");
        emailService.sendReply(user.getId(), id, replyBody);
        return ResponseEntity.ok(Map.of("message", "Reply sent successfully"));
    }
}
