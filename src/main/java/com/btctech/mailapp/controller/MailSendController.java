package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.dto.BulkMailRequest;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.MailSendService;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
import com.btctech.mailapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
public class MailSendController {

    private final MailSendService mailSendService;
    private final SessionService sessionService;
    private final MailboxService mailboxService;
    private final UserService userService;

    /**
     * Send email - NO PASSWORD NEEDED in request!
     * Authorization header provides context to retrieve password from session.
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendMail(
            @Valid @RequestBody SendMailRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            // Get email from authentication context
            String fromEmail = authentication.getName();
            log.info("Send mail request from {} to {}", fromEmail, request.getTo());

            // Extract JWT token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Missing or invalid Authorization header"));
            }
            String token = authHeader.substring(7);

            // Get password from backend session (Security: never expose password to client)
            String password = sessionService.getPasswordFromSession(token);

            if (password == null || password.isEmpty()) {
                log.error("Password not found in session for {}", fromEmail);
                return ResponseEntity.status(401)
                        .body(ApiResponse.error("Session expired or invalid. Please login again."));
            }

            // Verify user has this email account active
            MailAccount mailAccount = mailboxService.getMailAccountByEmail(fromEmail);
            if (mailAccount == null || !mailAccount.getActive()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Email account not found or disabled"));
            }

            // Execute send
            mailSendService.sendMail(fromEmail, password, request);

            // Response payload
            Map<String, Object> data = new HashMap<>();
            data.put("from", fromEmail);
            data.put("to", request.getTo());
            data.put("subject", request.getSubject());
            data.put("sentAt", System.currentTimeMillis());

            log.info("✓ Email sent successfully from {} to {}", fromEmail, request.getTo());

            return ResponseEntity.ok(
                    ApiResponse.success(data, "Email sent successfully"));

        } catch (MailException e) {
            log.error("Mail error: {}", e.getMessage());
            if (e.getMessage().contains("Session")) {
                return ResponseEntity.status(401).body(ApiResponse.error(e.getMessage()));
            }
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to send email: " + e.getMessage()));
        }
    }

    /**
     * Bulk send endpoint for marketing/offers.
     * Processes asynchronously.
     */
    @PostMapping("/bulk-send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bulkSend(
            @Valid @RequestBody BulkMailRequest request,
            @RequestHeader("Authorization") String authHeader,
            Authentication authentication) {

        try {
            String fromEmail = authentication.getName();
            log.info("Bulk send request from {} for {} recipients", fromEmail, request.getRecipients().size());

            // 1. Get the user and their mail account
            User user = userService.getUserByEmailOrUsername(fromEmail);
            
            // 2. Get password from ANY active session for this user
            String password = sessionService.getPasswordByUserId(user.getId());

            // 3. FALLBACK: If no session, try the persistent encrypted password
            if (password == null) {
                log.info("No session found for {}, trying persistent encrypted password", fromEmail);
                MailAccount mailAccount = mailboxService.getMailAccountByEmail(fromEmail);
                if (mailAccount.getEncryptedPassword() != null) {
                    try {
                        password = sessionService.decrypt(mailAccount.getEncryptedPassword());
                        log.info("✓ Recovered password from persistent storage for: {}", fromEmail);
                    } catch (Exception e) {
                        log.error("Failed to decrypt persistent password: {}", e.getMessage());
                    }
                }
            }

            if (password == null) {
                log.warn("No password source found for user: {}", fromEmail);
                return ResponseEntity.status(401).body(ApiResponse.error("Authentication required. Please login to B2Auth to re-sync your session."));
            }

            // Execute bulk send asynchronously
            mailSendService.sendBulkMail(fromEmail, password, request);

            Map<String, Object> data = new HashMap<>();
            data.put("status", "PROCESSING");
            data.put("recipientCount", request.getRecipients().size());

            return ResponseEntity.ok(ApiResponse.success(data, "Bulk email processing started"));

        } catch (Exception e) {
            log.error("Bulk send failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }
}