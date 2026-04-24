package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.service.MailSendService;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
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
}