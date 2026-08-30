package com.nexora.service;

import com.nexora.dto.request.DraftRequest;
import com.nexora.dto.response.DraftResponse;
import com.nexora.model.EmailDraft;
import com.nexora.model.User;
import com.nexora.repository.EmailDraftRepository;
import com.nexora.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * User-scoped drafts. Wire DTOs only — never serialize nested {@link User}.
 */
@Service
@RequiredArgsConstructor
public class EmailDraftService {

    private final EmailDraftRepository draftRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DraftResponse> getUserDrafts(Long userId) {
        requireUserId(userId);
        return draftRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(EmailDraftService::toResponse)
                .toList();
    }

    @Transactional
    public DraftResponse createDraft(Long userId, DraftRequest request) {
        requireUserId(userId);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft body is required");
        }

        User user = userRepository.getReferenceById(userId);
        EmailDraft draft = new EmailDraft();
        draft.setUser(user);
        applyRequest(draft, request);
        if (draft.getDraftStatus() == null || draft.getDraftStatus().isBlank()) {
            draft.setDraftStatus(draft.getScheduledSendTime() != null ? "SCHEDULED" : "DRAFT");
        }
        return toResponse(draftRepository.save(draft));
    }

    @Transactional
    public DraftResponse updateDraft(Long userId, Long id, DraftRequest request) {
        requireUserId(userId);
        requireId(id);
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft body is required");
        }

        EmailDraft existing = owned(userId, id);
        applyRequest(existing, request);
        if (existing.getDraftStatus() == null || existing.getDraftStatus().isBlank()) {
            existing.setDraftStatus(existing.getScheduledSendTime() != null ? "SCHEDULED" : "DRAFT");
        }
        return toResponse(draftRepository.save(existing));
    }

    @Transactional
    public void deleteDraft(Long userId, Long id) {
        requireUserId(userId);
        requireId(id);
        draftRepository.delete(owned(userId, id));
    }

    public String sendDraft(Long userId, Long id) {
        requireUserId(userId);
        requireId(id);
        owned(userId, id);
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Sending is unavailable: Cortex Mail holds read-only Gmail access. "
                        + "Drafts are stored here and can be copied into Gmail to send.");
    }

    private EmailDraft owned(Long userId, Long id) {
        return draftRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft " + id + " not found"));
    }

    private static void applyRequest(EmailDraft draft, DraftRequest request) {
        if (request.getTo() != null) draft.setTo(request.getTo());
        if (request.getCc() != null) draft.setCc(request.getCc());
        if (request.getBcc() != null) draft.setBcc(request.getBcc());
        if (request.getSubject() != null) draft.setSubject(request.getSubject());
        if (request.getBody() != null) draft.setBody(request.getBody());
        if (request.getHtmlBody() != null) draft.setHtmlBody(request.getHtmlBody());
        if (request.getScheduledSendTime() != null) draft.setScheduledSendTime(request.getScheduledSendTime());
        if (request.getDraftStatus() != null && !request.getDraftStatus().isBlank()) {
            draft.setDraftStatus(request.getDraftStatus().trim());
        }
    }

    private static void requireUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Draft id is required");
        }
    }

    private static DraftResponse toResponse(EmailDraft draft) {
        DraftResponse response = new DraftResponse();
        response.setId(draft.getId());
        response.setTo(draft.getTo());
        response.setCc(draft.getCc());
        response.setBcc(draft.getBcc());
        response.setSubject(draft.getSubject());
        response.setBody(draft.getBody());
        response.setHtmlBody(draft.getHtmlBody());
        response.setScheduledSendTime(draft.getScheduledSendTime());
        response.setDraftStatus(draft.getDraftStatus());
        response.setCreatedAt(draft.getCreatedAt());
        response.setUpdatedAt(draft.getUpdatedAt());
        return response;
    }
}
