package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.DraftRequest;
import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.MailDraft;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.MailAccountRepository;
import com.btctech.mailapp.repository.MailDraftRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailDraftService {

    private final MailDraftRepository draftRepository;
    private final MailAccountRepository mailAccountRepository;
    private final MailSendService mailSendService;
    private final FileStorageService fileStorageService;
    private final AttachmentSecurityService attachmentSecurityService;
    private final ImagePreviewService imagePreviewService;
    private final DraftCollaborationService collaborationService;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    /**
     * Upload attachment to draft
     */
    @Transactional
    public com.btctech.mailapp.dto.AttachmentInfo addAttachment(Long draftId, Long mailAccountId, Long userId, org.springframework.web.multipart.MultipartFile file) {
        MailDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new MailException("Draft not found"));

        // Collaborative Security: Owner OR Collaborator with EDIT permission
        boolean isOwner = Objects.equals(draft.getMailAccountId(), mailAccountId);
        boolean hasEditAccess = collaborationService.hasPermission(draftId, userId, com.btctech.mailapp.entity.CollaboratorPermission.EDIT);

        if (!isOwner && !hasEditAccess) {
            log.warn("Unauthorized ATTACHMENT attempt on Draft {} by User {}", draftId, userId);
            throw new MailException("You do not have permission to modify this draft");
        }

        // 1. Security Check (Virus Scan & Quota)
        attachmentSecurityService.validateQuota(mailAccountId, file.getSize());
        attachmentSecurityService.validateFile(file);

        // 2. Save to disk
        String filePath = fileStorageService.saveFile(draftId, file);
        
        // 3. Generate Thumbnail (Premium UX - V3)
        String thumbnailPath = null;
        if (imagePreviewService.isSupportedImage(file.getOriginalFilename())) {
            thumbnailPath = imagePreviewService.generateThumbnail(draftId, filePath, file.getOriginalFilename());
        }

        // 4. Update Database (Atomic Storage Increase)
        mailAccountRepository.updateStorageUsed(mailAccountId, file.getSize());

        // 5. Update JSON Manifest
        com.btctech.mailapp.dto.AttachmentInfo info = new com.btctech.mailapp.dto.AttachmentInfo(
                file.getOriginalFilename(),
                filePath,
                thumbnailPath,
                file.getSize()
        );

        try {
            java.util.List<com.btctech.mailapp.dto.AttachmentInfo> attachments = getAttachmentsList(draft.getAttachmentsJson());
            attachments.add(info);
            draft.setAttachmentsJson(objectMapper.writeValueAsString(attachments));
            draftRepository.save(draft);
        } catch (Exception e) {
            log.error("Failed to update draft attachments JSON: {}", e.getMessage());
            throw new MailException("Failed to update attachment metadata");
        }

        return info;
    }

    /**
     * Remove attachment from draft
     */
    @Transactional
    public void removeAttachment(Long draftId, Long mailAccountId, Long userId, String fileName) {
        MailDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new MailException("Draft not found"));

        // Collaborative Security: Owner OR Collaborator with EDIT permission
        boolean isOwner = Objects.equals(draft.getMailAccountId(), mailAccountId);
        boolean hasEditAccess = collaborationService.hasPermission(draftId, userId, com.btctech.mailapp.entity.CollaboratorPermission.EDIT);

        if (!isOwner && !hasEditAccess) {
            log.warn("Unauthorized ATTACHMENT REMOVAL attempt on Draft {} by User {}", draftId, userId);
            throw new MailException("You do not have permission to modify this draft");
        }

        try {
            java.util.List<com.btctech.mailapp.dto.AttachmentInfo> attachments = getAttachmentsList(draft.getAttachmentsJson());
            java.util.Optional<com.btctech.mailapp.dto.AttachmentInfo> toRemove = attachments.stream()
                    .filter(a -> a.getFileName().equals(fileName))
                    .findFirst();

            if (toRemove.isPresent()) {
                fileStorageService.deleteFile(toRemove.get().getFilePath());
                attachments.remove(toRemove.get());
                draft.setAttachmentsJson(objectMapper.writeValueAsString(attachments));
                draftRepository.save(draft);

                // Reclaim Storage Space
                mailAccountRepository.updateStorageUsed(mailAccountId, -toRemove.get().getSize());
            }
        } catch (Exception e) {
            log.error("Failed to remove attachment: {}", e.getMessage());
            throw new MailException("Failed to remove attachment");
        }
    }

    /**
     * Helper to deserialize attachments
     */
    private java.util.List<com.btctech.mailapp.dto.AttachmentInfo> getAttachmentsList(String json) throws Exception {
        if (json == null || json.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<com.btctech.mailapp.dto.AttachmentInfo>>() {});
    }

    /**
     * Save or update a draft (Idempotent Autosave)
     */
    @Transactional
    public MailDraft saveOrUpdateDraft(DraftRequest request) {
        MailDraft draft;
        
        if (request.getId() != null) {
            // 1. Initial lookup
            draft = draftRepository.findById(request.getId())
                    .orElseThrow(() -> new MailException("Draft not found"));

            // 2. Collaborative Security: Owner OR Collaborator with EDIT permission
            boolean isOwner = Objects.equals(draft.getMailAccountId(), request.getMailAccountId());
            boolean hasEditAccess = collaborationService.hasPermission(request.getId(), request.getUserId(), com.btctech.mailapp.entity.CollaboratorPermission.EDIT);

            if (!isOwner && !hasEditAccess) {
                log.warn("Unauthorized EDIT attempt on Draft {} by User {}", request.getId(), request.getUserId());
                throw new MailException("You do not have permission to edit this draft");
            }
        } else {
            draft = new MailDraft();
            draft.setMailAccountId(request.getMailAccountId());
        }

        draft.setTo(request.getTo());
        draft.setCc(request.getCc());
        draft.setBcc(request.getBcc());
        draft.setSubject(request.getSubject());
        draft.setBody(request.getBody());
        draft.setIsHtml(request.getIsHtml());

        draft = draftRepository.save(draft);
        log.info("✓ Draft saved successfully. ID: {}, accountId: {}", draft.getId(), draft.getMailAccountId());
        
        return draft;
    }

    /**
     * Get a draft for editing (Triggers UX LastOpened update)
     */
    @Transactional
    public MailDraft getDraftForEditing(Long id, Long mailAccountId, Long userId) {
        MailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new MailException("Draft not found"));

        // Collaborative Security: Owner OR Collaborator with VIEW permission
        boolean isOwner = Objects.equals(draft.getMailAccountId(), mailAccountId);
        boolean hasViewAccess = collaborationService.hasPermission(id, userId, com.btctech.mailapp.entity.CollaboratorPermission.VIEW);

        if (!isOwner && !hasViewAccess) {
             log.warn("Unauthorized VIEW attempt on Draft {} by User {}", id, userId);
             throw new MailException("Draft not found"); // Stealth 404
        }
        
        draft.setLastOpenedAt(LocalDateTime.now());
        return draftRepository.save(draft);
    }

    /**
     * Get all drafts for a mail account
     */
    public java.util.List<MailDraft> getAccountDrafts(Long mailAccountId) {
        return draftRepository.findByMailAccountIdOrderByUpdatedAtDesc(mailAccountId);
    }

    /**
     * Secure delete a draft
     */
    @Transactional
    public void deleteDraft(Long id, Long mailAccountId) {
        MailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new MailException("Draft not found"));

        if (!Objects.equals(draft.getMailAccountId(), mailAccountId)) {
            log.warn("Unauthorized DELETE attempt on Draft {} by Non-Owner account {}", id, mailAccountId);
            throw new MailException("Only the draft owner can delete this draft");
        }
        
        // 1. Calculate reclamation size
        try {
            java.util.List<com.btctech.mailapp.dto.AttachmentInfo> attachments = getAttachmentsList(draft.getAttachmentsJson());
            long totalSize = attachments.stream().mapToLong(com.btctech.mailapp.dto.AttachmentInfo::getSize).sum();
            if (totalSize > 0) {
                mailAccountRepository.updateStorageUsed(mailAccountId, -totalSize);
            }
        } catch (Exception e) {
            log.warn("Failed to calculate storage reclamation for draft {}: {}", id, e.getMessage());
        }

        // 2. Delete files from disk
        fileStorageService.deleteDraftDirectory(id);
        
        // 3. Delete DB record
        draftRepository.delete(draft);
    }

    /**
     * Send a draft
     */
    @Transactional
    public void sendDraft(Long id, Long mailAccountId, Long userId) {
        // 1. Initial lookup
        MailDraft draft = draftRepository.findById(id)
                .orElseThrow(() -> new MailException("Draft not found"));

        // 2. Collaborative Security: Owner OR Collaborator with SEND permission
        boolean isOwner = Objects.equals(draft.getMailAccountId(), mailAccountId);
        boolean hasSendAccess = collaborationService.hasPermission(id, userId, com.btctech.mailapp.entity.CollaboratorPermission.SEND);

        if (!isOwner && !hasSendAccess) {
            log.warn("Unauthorized SEND attempt on Draft {} by User {}", id, userId);
            throw new MailException("You do not have permission to send this draft");
        }

        // 3. Get Mail Account for credentials
        MailAccount account = mailAccountRepository.findById(draft.getMailAccountId())
                .orElseThrow(() -> new MailException("Mail account not found"));

        // 3. Convert to SendMailRequest
        SendMailRequest sendRequest = new SendMailRequest();
        sendRequest.setTo(draft.getTo());
        sendRequest.setCc(draft.getCc());
        sendRequest.setBcc(draft.getBcc());
        sendRequest.setSubject(draft.getSubject());
        sendRequest.setBody(draft.getBody());
        sendRequest.setIsHtml(draft.getIsHtml());

        // 4. Attach Files from JSON manifest
        try {
            sendRequest.setAttachments(getAttachmentsList(draft.getAttachmentsJson()));
        } catch (Exception e) {
            log.warn("Failed to parse attachments for draft {}: {}", id, e.getMessage());
        }

        // 5. Lifecycle Check: Prevent duplicate sending
        if ("SENDING".equalsIgnoreCase(draft.getStatus())) {
            throw new MailException("Draft is already being sent. Please wait.");
        }

        // 6. Set Status to SENDING
        draft.setStatus("SENDING");
        draft.setFailureReason(null);
        draftRepository.save(draft);

        try {
            // 7. Send
            mailSendService.sendMail(account.getEmail(), account.getPassword(), sendRequest);

            // 8. Reclaim Storage (Total size of all attachments combined)
            long totalReclaim = sendRequest.getAttachments().stream().mapToLong(com.btctech.mailapp.dto.AttachmentInfo::getSize).sum();
            if (totalReclaim > 0) {
                mailAccountRepository.updateStorageUsed(mailAccountId, -totalReclaim);
            }

            // 9. Delete draft and files on success
            fileStorageService.deleteDraftDirectory(id);
            draftRepository.delete(draft);
            log.info("✓ Draft {} sent and cleaned up successfully", id);

        } catch (Exception e) {
            log.error("Failed to send draft {}: {}", id, e.getMessage());
            
            // 10. FAILURE RECOVERY: Restore to FAILED mode instead of deleting
            draft.setStatus("FAILED");
            draft.setFailureReason(e.getMessage());
            draftRepository.save(draft);
            
            throw new MailException("Failed to send draft: " + e.getMessage());
        }
    }
}
