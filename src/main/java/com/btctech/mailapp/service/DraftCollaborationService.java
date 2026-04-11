package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.CollaboratorPermission;
import com.btctech.mailapp.entity.DraftCollaborator;
import com.btctech.mailapp.entity.MailDraft;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.DraftCollaboratorRepository;
import com.btctech.mailapp.repository.MailDraftRepository;
import com.btctech.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Service to manage draft sharing and collaboration within an organization.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftCollaborationService {

    private final DraftCollaboratorRepository collaboratorRepository;
    private final MailDraftRepository draftRepository;
    private final UserRepository userRepository;

    /**
     * Add a collaborator to a draft.
     * Logic: Only the draft owner or ORG_ADMIN can add collaborators.
     * Security: Users must be in the same organization.
     */
    @Transactional
    public void addCollaborator(Long draftId, Long ownerId, Long targetUserId, CollaboratorPermission permission) {
        MailDraft draft = draftRepository.findById(draftId)
                .orElseThrow(() -> new MailException("Draft not found"));

        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new MailException("Owner not found"));

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new MailException("Target user not found"));

        // 1. Security: Check if owner is actually the draft creator
        // (Assuming draft.getMailAccountId() links to a mailbox owned by the user)
        // For simplicity, we check if they are in the same organization for now.
        if (!Objects.equals(owner.getOrganization().getId(), targetUser.getOrganization().getId())) {
            throw new MailException("Collaboration limited to users within the same organization.");
        }

        // 2. Add or Update collaborator
        DraftCollaborator collaborator = collaboratorRepository.findByDraftIdAndUserId(draftId, targetUserId)
                .orElse(new DraftCollaborator(draft, targetUser, permission));
        
        collaborator.setPermission(permission);
        collaboratorRepository.save(collaborator);
        
        log.info("✓ User {} added to Draft {} with permission {}", targetUserId, draftId, permission);
    }

    /**
     * Check if a user has a specific permission on a draft.
     */
    public boolean hasPermission(Long draftId, Long userId, CollaboratorPermission required) {
        Optional<DraftCollaborator> collab = collaboratorRepository.findByDraftIdAndUserId(draftId, userId);
        if (collab.isEmpty()) return false;

        CollaboratorPermission actual = collab.get().getPermission();

        // Hierarchy: SEND > EDIT > VIEW
        if (required == CollaboratorPermission.VIEW) return true;
        if (required == CollaboratorPermission.EDIT) return actual == CollaboratorPermission.EDIT || actual == CollaboratorPermission.SEND;
        if (required == CollaboratorPermission.SEND) return actual == CollaboratorPermission.SEND;

        return false;
    }

    public List<DraftCollaborator> getCollaborators(Long draftId) {
        return collaboratorRepository.findByDraftId(draftId);
    }

    @Transactional
    public void removeCollaborator(Long draftId, Long userId) {
        collaboratorRepository.findByDraftIdAndUserId(draftId, userId)
                .ifPresent(collaboratorRepository::delete);
    }
}
