package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.EmailDTO;
import com.btctech.mailapp.dto.InboxResponse;
import com.btctech.mailapp.service.MailReceiveService;
import com.btctech.mailapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailReceiveController {

    private final MailReceiveService mailReceiveService;
    private final SessionService sessionService;

    /**
     * Get inbox emails
     */
    @GetMapping("/inbox")
    public ResponseEntity<ApiResponse<InboxResponse>> getInbox(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get inbox request from: {}", email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            // Fetch emails
            List<EmailDTO> emails = mailReceiveService.getInbox(email, password, limit);

            // Get unread count
            int unreadCount = mailReceiveService.getUnreadCount(email, password);

            // Build response
            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(unreadCount)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Inbox fetched successfully"));

        } catch (Exception e) {
            log.error("Error fetching inbox: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch inbox: " + e.getMessage()));
        }
    }

    /**
     * Get single email
     */
    @GetMapping("/email/{uid}")
    public ResponseEntity<ApiResponse<EmailDTO>> getEmail(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get email {} request from: {}", uid, email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            // Fetch email
            EmailDTO emailDTO = mailReceiveService.getEmail(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(emailDTO, "Email fetched successfully"));

        } catch (Exception e) {
            log.error("Error fetching email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch email: " + e.getMessage()));
        }
    }

    /**
     * Mark email as read
     */
    @PostMapping("/read/{uid}")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Mark as read request for {} from {}", uid, email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.markAsRead(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email marked as read"));

        } catch (Exception e) {
            log.error("Error marking email as read: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to mark email as read: " + e.getMessage()));
        }
    }
}