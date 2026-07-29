package com.nexora.service;

import com.nexora.model.EmailDraft;
import com.nexora.model.User;
import com.nexora.repository.EmailDraftRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailDraftService {

    private final EmailDraftRepository draftRepository;

    public List<EmailDraft> getUserDrafts(Long userId) {
        return draftRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Transactional
    public EmailDraft createDraft(User user, EmailDraft draft) {
        draft.setId(null);
        draft.setUser(user);
        if (draft.getDraftStatus() == null || draft.getDraftStatus().isBlank()) {
            draft.setDraftStatus(draft.getScheduledSendTime() != null ? "SCHEDULED" : "DRAFT");
        }
        return draftRepository.save(draft);
    }

    @Transactional
    public EmailDraft updateDraft(User user, Long id, EmailDraft incoming) {
        EmailDraft existing = owned(user, id);
        existing.setTo(incoming.getTo());
        existing.setCc(incoming.getCc());
        existing.setBcc(incoming.getBcc());
        existing.setSubject(incoming.getSubject());
        existing.setBody(incoming.getBody());
        existing.setHtmlBody(incoming.getHtmlBody());
        existing.setScheduledSendTime(incoming.getScheduledSendTime());
        if (incoming.getDraftStatus() != null && !incoming.getDraftStatus().isBlank()) {
            existing.setDraftStatus(incoming.getDraftStatus());
        }
        return draftRepository.save(existing);
    }

    @Transactional
    public void deleteDraft(User user, Long id) {
        draftRepository.delete(owned(user, id));
    }

    /**
     * Sending is intentionally not implemented. Velocity holds a read-only
     * Gmail scope, which is the guarantee made to the user at sign-in — it can
     * read mail but cannot send, delete or alter it. Wiring a send path here
     * would require widening that scope, so this fails loudly rather than
     * appearing to work.
     */
    public String sendDraft(User user, Long id) {
        owned(user, id);
        throw new ResponseStatusException(
                HttpStatus.NOT_IMPLEMENTED,
                "Sending is unavailable: Velocity holds read-only Gmail access. "
                        + "Drafts are stored here and can be copied into Gmail to send.");
    }

    private EmailDraft owned(User user, Long id) {
        return draftRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Draft " + id + " not found"));
    }
}
