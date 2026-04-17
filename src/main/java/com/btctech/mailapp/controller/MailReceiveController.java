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
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error fetching inbox for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch inbox: " + e.getMessage()));
        }
    }

    /**
     * Get sent emails
     */
    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<InboxResponse>> getSent(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get sent emails request from: {}", email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            // Fetch emails
            List<EmailDTO> emails = mailReceiveService.getSent(email, password, limit);

            // Build response
            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0) // Sent items don't really have "unread" count in this context
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} sent emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Sent emails fetched successfully"));

        } catch (Exception e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error fetching sent emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch sent emails: " + e.getMessage()));
        }
    }

    /**
     * Get starred emails
     */
    @GetMapping("/starred")
    public ResponseEntity<ApiResponse<InboxResponse>> getStarred(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get starred emails request from: {}", email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            // Fetch emails
            List<EmailDTO> emails = mailReceiveService.getStarred(email, password, limit);

            // Build response
            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} starred emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Starred emails fetched successfully"));

        } catch (Exception e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error fetching starred emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch starred emails: " + e.getMessage()));
        }
    }

    /**
     * Toggle starred status
     */
    @PostMapping("/star/{uid}")
    public ResponseEntity<ApiResponse<Void>> toggleStar(
            @PathVariable String uid,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Toggle star request for {} in folder {} from {}", uid, folder, email);

            // Get password from session
            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.toggleStar(email, password, folder, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Star status toggled successfully"));

        } catch (Exception e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error toggling star status for UID {} in folder {} for {}: {}", uid, folder, userEmail, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to toggle star status: " + e.getMessage()));
        }
    }

    /**
     * Get trash emails
     */
    @GetMapping("/trash")
    public ResponseEntity<ApiResponse<InboxResponse>> getTrash(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get trash emails request from: {}", email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            List<EmailDTO> emails = mailReceiveService.getTrash(email, password, limit);

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} trash emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Trash emails fetched successfully"));

        } catch (Exception e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error fetching trash emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to fetch trash emails: " + e.getMessage()));
        }
    }

    /**
     * Move to trash
     */
    @PostMapping("/trash/{uid}")
    public ResponseEntity<ApiResponse<Void>> moveToTrash(
            @PathVariable String uid,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Move to trash request for {} in folder {} from {}", uid, folder, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.moveToTrash(email, password, folder, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email moved to trash successfully"));

        } catch (Exception e) {
            log.error("Error moving email UID {} to trash from {}: {}", uid, folder, e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to move email to trash: " + e.getMessage()));
        }
    }

    /**
     * Restore from trash
     */
    @PostMapping("/restore/{uid}")
    public ResponseEntity<ApiResponse<Void>> restoreFromTrash(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Restore from trash request for {} from {}", uid, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.restoreFromTrash(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email restored successfully"));

        } catch (Exception e) {
            log.error("Error restoring email from trash: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to restore email: " + e.getMessage()));
        }
    }

    /**
     * Delete permanently
     */
    @DeleteMapping("/permanent/{uid}")
    public ResponseEntity<ApiResponse<Void>> deletePermanently(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Permanent delete request for {} from {}", uid, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.deletePermanently(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email deleted permanently"));

        } catch (Exception e) {
            log.error("Error deleting email permanently: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to delete email permanently: " + e.getMessage()));
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
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("Error fetching email {} for {}: {}", uid, userEmail, e.getMessage(), e);
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

    /**
     * Download attachment from a received email
     */
    @GetMapping("/{uid}/attachments/{fileName}")
    public void downloadAttachment(
            @PathVariable String uid,
            @PathVariable String fileName,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication,
            jakarta.servlet.http.HttpServletResponse response) {
        
        String email = authentication.getName();
        String token = authHeader.substring(7);
        String password = sessionService.getPasswordFromSession(token);
        
        log.info("Download attachment request: {} from email: {} for user: {}", fileName, uid, email);

        if (password == null) {
            log.warn("Unauthorized download attempt (expired session) by {}", email);
            response.setStatus(401);
            return;
        }

        try {
            // Set response headers
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            
            // Stream the attachment
            mailReceiveService.downloadAttachment(
                email, 
                password, 
                uid, 
                fileName, 
                response.getOutputStream()
            );
            
            response.flushBuffer();
        } catch (Exception e) {
            log.error("Failed to stream attachment: {}", e.getMessage());
            if (!response.isCommitted()) {
                response.setStatus(500);
            }
        }
    }
}