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
            @RequestParam(required = false) String category,
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
            if (emails == null) emails = new java.util.ArrayList<>();

            // Filter by category if provided
            if (category != null && !category.isEmpty()) {
                emails = emails.stream()
                        .filter(e -> category.equalsIgnoreCase(e.getCategory()))
                        .toList();
                log.info("Filtered inbox to {} emails for category: {}", emails.size(), category);
            }

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

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching inbox for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch inbox: " + e.getMessage()));
        }
    }

    /**
     * Get emails by category (Social, Promotions, Updates, etc.)
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<InboxResponse>> getEmailsByCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {
        
        try {
            String email = authentication.getName();
            log.info("Fetching emails for category: {} for user: {}", category, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            List<EmailDTO> emails = mailReceiveService.getEmailsByCategory(email, password, category, limit);

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0) // Logic for per-category unread count can be added later
                    .emails(emails)
                    .build();

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Category " + category + " fetched successfully"));

        } catch (Throwable e) {
            log.error("CRITICAL error fetching category {}: {}", category, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch categorized emails: " + e.getMessage()));
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
            if (emails == null) emails = new java.util.ArrayList<>();

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

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching sent emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch sent emails: " + e.getMessage()));
        }
    }

    /**
     * Get emails by label
     */
    @GetMapping("/labels/{labelId}")
    public ResponseEntity<ApiResponse<InboxResponse>> getEmailsByLabel(
            @PathVariable Long labelId,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get emails for label {} request from: {}", labelId, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            List<EmailDTO> emails = mailReceiveService.getEmailsByLabel(email, password, labelId);
            if (emails == null) emails = new java.util.ArrayList<>();

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} emails for label {} for {}", emails.size(), labelId, email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Emails for label fetched successfully"));

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching emails for label {} for {}: {}", labelId, userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch emails for this label: " + e.getMessage()));
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
            if (emails == null) emails = new java.util.ArrayList<>();

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

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching starred emails for {}: {}", userEmail, e.getMessage(), e);
            
            // Temporary verbose error for remote debugging
            String stackTrace = java.util.Arrays.toString(e.getStackTrace());
            String debugMessage = String.format("[%s] %s | Stack: %s", 
                e.getClass().getSimpleName(), 
                e.getMessage(), 
                stackTrace.substring(0, Math.min(stackTrace.length(), 200)));

            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Debug Error: " + debugMessage));
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

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error toggling star status for UID {} in folder {} for {}: {}", uid, folder, userEmail, e.getMessage(), e);
            
            // Temporary verbose error for remote debugging
            String stackTrace = java.util.Arrays.toString(e.getStackTrace());
            String debugMessage = String.format("[%s] %s | Stack: %s", 
                e.getClass().getSimpleName(), 
                e.getMessage(), 
                stackTrace.substring(0, Math.min(stackTrace.length(), 200)));

            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Debug Error: " + debugMessage));
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
            if (emails == null) emails = new java.util.ArrayList<>();

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} trash emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Trash emails fetched successfully"));

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching trash emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch trash emails: " + e.getMessage()));
        }
    }

    /**
     * Get spam emails
     */
    @GetMapping("/spam")
    public ResponseEntity<ApiResponse<InboxResponse>> getSpam(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get spam emails request from: {}", email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            List<EmailDTO> emails = mailReceiveService.getSpam(email, password, limit);
            if (emails == null) emails = new java.util.ArrayList<>();

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} spam emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Spam emails fetched successfully"));

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching spam emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch spam emails: " + e.getMessage()));
        }
    }

    /**
     * Get snoozed emails
     */
    @GetMapping("/snoozed")
    public ResponseEntity<ApiResponse<InboxResponse>> getSnoozed(
            @RequestParam(defaultValue = "50") int limit,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Get snoozed emails request from: {}", email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            List<EmailDTO> emails = mailReceiveService.getSnoozed(email, password, limit);
            if (emails == null) emails = new java.util.ArrayList<>();

            InboxResponse response = InboxResponse.builder()
                    .email(email)
                    .totalCount(emails.size())
                    .unreadCount(0)
                    .emails(emails)
                    .build();

            log.info("✓ Fetched {} snoozed emails for {}", emails.size(), email);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Snoozed emails fetched successfully"));

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching snoozed emails for {}: {}", userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to fetch snoozed emails: " + e.getMessage()));
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

        } catch (Throwable e) {
            log.error("CRITICAL error moving email UID {} to trash from {}: {}", uid, folder, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to move email to trash: " + e.getMessage()));
        }
    }

    /**
     * Mark as spam
     */
    @PostMapping("/spam/{uid}")
    public ResponseEntity<ApiResponse<Void>> markAsSpam(
            @PathVariable String uid,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Mark as spam request for {} in folder {} from {}", uid, folder, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.markAsSpam(email, password, folder, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email marked as spam successfully"));

        } catch (Throwable e) {
            log.error("CRITICAL error marking email UID {} as spam from {}: {}", uid, folder, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to mark as spam: " + e.getMessage()));
        }
    }

    /**
     * Snooze email
     */
    @PostMapping("/snooze/{uid}")
    public ResponseEntity<ApiResponse<Void>> snoozeEmail(
            @PathVariable String uid,
            @RequestParam String wakeUpAt,
            @RequestParam(defaultValue = "INBOX") String folder,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Snooze request for {} in folder {} until {}", uid, folder, wakeUpAt);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            java.time.LocalDateTime wakeTime = java.time.LocalDateTime.parse(wakeUpAt);
            mailReceiveService.snoozeEmail(email, password, folder, uid, wakeTime);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email snoozed successfully"));

        } catch (Exception e) {
            log.error("CRITICAL error snoozing email UID {}: {}", uid, e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to snooze email: " + e.getMessage()));
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

        } catch (Throwable e) {
            log.error("CRITICAL error restoring email from trash: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to restore email: " + e.getMessage()));
        }
    }

    /**
     * Restore from spam
     */
    @PostMapping("/restore-spam/{uid}")
    public ResponseEntity<ApiResponse<Void>> restoreFromSpam(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Restore from spam request for {} from {}", uid, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.restoreFromSpam(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email restored from spam successfully"));

        } catch (Throwable e) {
            log.error("CRITICAL error restoring email from spam: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to restore from spam: " + e.getMessage()));
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

        } catch (Throwable e) {
            log.error("CRITICAL error deleting email permanently: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
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

        } catch (Throwable e) {
            String userEmail = (authentication != null) ? authentication.getName() : "Unknown User";
            log.error("CRITICAL error fetching email {} for {}: {}", uid, userEmail, e.getMessage(), e);
            return ResponseEntity.status(500)
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

        } catch (Throwable e) {
            log.error("CRITICAL error marking email as read: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to mark email as read: " + e.getMessage()));
        }
    }

    /**
     * Mark email as unread
     */
    @PostMapping("/unread/{uid}")
    public ResponseEntity<ApiResponse<Void>> markAsUnread(
            @PathVariable String uid,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String email = authentication.getName();
            log.info("Mark as unread request for {} from {}", uid, email);

            String token = authHeader.substring(7);
            String password = sessionService.getPasswordFromSession(token);

            if (password == null) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired. Please login again."));
            }

            mailReceiveService.markAsUnread(email, password, uid);

            return ResponseEntity.ok(
                    ApiResponse.success(null, "Email marked as unread"));

        } catch (Throwable e) {
            log.error("CRITICAL error marking email as unread: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("Failed to mark email as unread: " + e.getMessage()));
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