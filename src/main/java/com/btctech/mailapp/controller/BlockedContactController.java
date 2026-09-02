package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.BlockedContactDTO;
import com.btctech.mailapp.service.BlockedContactService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/blocked-contacts")
@RequiredArgsConstructor
public class BlockedContactController {

    private final BlockedContactService blockedContactService;

    /**
     * Block a sender address for the authenticated user.
     * POST /api/blocked-contacts
     * Request body: { "email": "john@gmail.com" }
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> blockSender(
            @RequestBody Map<String, String> payload,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: User authentication required"));
            }

            String userEmail = authentication.getName();
            String email = payload != null ? payload.get("email") : null;

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Email address is required"));
            }

            blockedContactService.blockSender(userEmail, email);
            return ResponseEntity.ok(ApiResponse.success(null, "Sender blocked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error blocking sender: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to block sender: " + e.getMessage()));
        }
    }

    /**
     * Unblock a sender address for the authenticated user.
     * DELETE /api/blocked-contacts/{email}
     */
    @DeleteMapping("/{email}")
    public ResponseEntity<ApiResponse<Void>> unblockSender(
            @PathVariable String email,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: User authentication required"));
            }

            String userEmail = authentication.getName();
            blockedContactService.unblockSender(userEmail, email);
            return ResponseEntity.ok(ApiResponse.success(null, "Sender unblocked successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error unblocking sender: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(ApiResponse.error("Failed to unblock sender: " + e.getMessage()));
        }
    }

    /**
     * Check block status of a sender for the authenticated user.
     * GET /api/blocked-contacts/check?email=john@gmail.com
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkBlockStatus(
            @RequestParam(required = false) String email,
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: User authentication required"));
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(Map.of("blocked", false), "No email provided"));
            }

            String userEmail = authentication.getName();
            boolean isBlocked = blockedContactService.isSenderBlocked(userEmail, email);
            return ResponseEntity.ok(ApiResponse.success(Map.of("blocked", isBlocked), "Block status retrieved"));
        } catch (Exception e) {
            log.error("Error checking block status for email '{}': {}", email, e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(Map.of("blocked", false), "Fallback block status"));
        }
    }

    /**
     * Get all blocked contacts for the authenticated user.
     * GET /api/blocked-contacts
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BlockedContactDTO>>> getBlockedContacts(
            Authentication authentication) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized: User authentication required"));
            }

            String userEmail = authentication.getName();
            List<BlockedContactDTO> contacts = blockedContactService.getBlockedContacts(userEmail);
            return ResponseEntity.ok(ApiResponse.success(contacts, "Blocked contacts fetched successfully"));
        } catch (Exception e) {
            log.error("Error fetching blocked contacts: {}", e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.success(List.of(), "Fetched blocked contacts fallback"));
        }
    }
}
