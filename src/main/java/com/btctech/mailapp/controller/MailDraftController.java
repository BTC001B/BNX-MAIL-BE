package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.DraftRequest;
import com.btctech.mailapp.entity.MailDraft;
import com.btctech.mailapp.service.MailDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mail/drafts")
@RequiredArgsConstructor
public class MailDraftController {

    private final MailDraftService draftService;
    private final com.btctech.mailapp.repository.UserSessionRepository sessionRepository;

    /**
     * Helper to get authorized session from JWT
     * Enforces Zero-Trust by extracting account context from token
     */
    private com.btctech.mailapp.entity.UserSession getAuthorizedSession(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new com.btctech.mailapp.exception.MailException("Missing or invalid Authorization header");
        }
        String token = authHeader.substring(7);
        return sessionRepository.findByJwtToken(token)
                .orElseThrow(() -> new com.btctech.mailapp.exception.MailException("Session not found or expired"));
    }

    /**
     * Save or update draft (Idempotent Autosave)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<MailDraft>> saveDraft(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody DraftRequest request) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        
        log.info("Save/Autosave draft request for session: {}", session.getId());
        
        // Security: Override DTO context with Session data to prevent spoofing
        request.setMailAccountId(session.getMailAccountId());
        request.setUserId(session.getUserId());
        
        MailDraft savedDraft = draftService.saveOrUpdateDraft(request);
        return ResponseEntity.ok(ApiResponse.success(savedDraft, "Draft saved successfully"));
    }

    /**
     * Get draft for editing (Secure + UX Open update)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MailDraft>> getDraft(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Opening draft {} for account: {}", id, session.getMailAccountId());
        
        MailDraft draft = draftService.getDraftForEditing(id, session.getMailAccountId(), session.getUserId());
        return ResponseEntity.ok(ApiResponse.success(draft, "Draft retrieved successfully"));
    }

    /**
     * List drafts for the authenticated account
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<MailDraft>>> listDrafts(
            @RequestHeader("Authorization") String authHeader) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Listing drafts for account: {}", session.getMailAccountId());
        
        List<MailDraft> drafts = draftService.getAccountDrafts(session.getMailAccountId());
        return ResponseEntity.ok(ApiResponse.success(drafts, "Drafts retrieved successfully"));
    }

    /**
     * Secure Delete a draft
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDraft(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Delete draft: {} for account: {}", id, session.getMailAccountId());
        
        draftService.deleteDraft(id, session.getMailAccountId());
        return ResponseEntity.ok(ApiResponse.success(null, "Draft deleted successfully"));
    }

    /**
     * Upload attachment to draft
     */
    @PostMapping("/{id}/attachments")
    public ResponseEntity<ApiResponse<com.btctech.mailapp.dto.AttachmentInfo>> uploadAttachment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Uploading attachment to draft: {} for account: {}", id, session.getMailAccountId());
        
        com.btctech.mailapp.dto.AttachmentInfo info = draftService.addAttachment(id, session.getMailAccountId(), session.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.success(info, "Attachment uploaded successfully"));
    }

    /**
     * Remove attachment from draft
     */
    @DeleteMapping("/{id}/attachments/{fileName}")
    public ResponseEntity<ApiResponse<Void>> removeAttachment(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @PathVariable String fileName) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Removing attachment {} from draft: {} for account: {}", fileName, id, session.getMailAccountId());
        
        draftService.removeAttachment(id, session.getMailAccountId(), session.getUserId(), fileName);
        return ResponseEntity.ok(ApiResponse.success(null, "Attachment removed successfully"));
    }

    /**
     * Secure Send a draft
     */
    @PostMapping("/{id}/send")
    public ResponseEntity<ApiResponse<Void>> sendDraft(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Send draft request: {} for account: {}", id, session.getMailAccountId());
        
        draftService.sendDraft(id, session.getMailAccountId(), session.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Draft sent and deleted successfully"));
    }
}
