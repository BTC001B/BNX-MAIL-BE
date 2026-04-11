package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.CollaboratorPermission;
import com.btctech.mailapp.entity.DraftCollaborator;
import com.btctech.mailapp.service.DraftCollaborationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for managing draft sharing and collaboration.
 */
@Slf4j
@RestController
@RequestMapping("/api/mail/drafts/{id}/collaborators")
@RequiredArgsConstructor
public class DraftCollaborationController {

    private final DraftCollaborationService collaborationService;
    private final com.btctech.mailapp.repository.UserSessionRepository sessionRepository;

    /**
     * Helper to get authorized session from JWT
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
     * Add a collaborator to a draft
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> addCollaborator(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody CollaboratorRequest request) {
        
        com.btctech.mailapp.entity.UserSession session = getAuthorizedSession(authHeader);
        log.info("Adding collaborator {} to draft {} by session user {}", request.getUserId(), id, session.getUserId());
        
        collaborationService.addCollaborator(id, session.getUserId(), request.getUserId(), request.getPermission());
        return ResponseEntity.ok(ApiResponse.success(null, "Collaborator added successfully"));
    }

    /**
     * List all collaborators for a draft
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DraftCollaborator>>> getCollaborators(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        getAuthorizedSession(authHeader); // Validate session
        List<DraftCollaborator> collaborators = collaborationService.getCollaborators(id);
        return ResponseEntity.ok(ApiResponse.success(collaborators, "Collaborators retrieved successfully"));
    }

    /**
     * Remove a collaborator
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeCollaborator(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @PathVariable Long userId) {
        
        getAuthorizedSession(authHeader); // Validate session
        collaborationService.removeCollaborator(id, userId);
        return ResponseEntity.ok(ApiResponse.success(null, "Collaborator removed successfully"));
    }

    @lombok.Data
    public static class CollaboratorRequest {
        private Long userId;
        private CollaboratorPermission permission;
    }
}
