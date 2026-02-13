package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.CreateEmailRequest;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
import com.btctech.mailapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/emails")
@RequiredArgsConstructor
public class EmailManagementController {

    private final MailboxService mailboxService;
    private final UserService userService;
    private final SessionService sessionService; // ✅ ADD THIS
    private final JwtUtil jwtUtil;

    /**
     * STEP 2: Create custom email (uses tempToken from registration)
     */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createEmail(
            @Valid @RequestBody CreateEmailRequest request,
            @RequestHeader("Authorization") String authHeader) {

        try {
            // Extract token
            String token = authHeader.substring(7);
            String tokenSubject = jwtUtil.extractEmail(token);

            log.info("Create email request: {} by token subject: {}", request.getEmailName(), tokenSubject);

            // Get user and password based on token type
            User user;
            String plainPassword;

            if (tokenSubject.startsWith("temp_")) {
                // TEMP TOKEN (from registration)
                String username = tokenSubject.substring(5); // Remove "temp_"
                user = userService.getUserByUsername(username);
                
                // ✅ FIX: Get password from request body
                plainPassword = request.getPassword();
                
                if (plainPassword == null || plainPassword.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Password is required for email creation"));
                }
                
                log.info("Creating email for user: {} (via temp token)", username);
                
            } else {
                // REGULAR TOKEN (from login)
                user = userService.getUserByEmail(tokenSubject);
                
                // ✅ FIX: Get password from session
                plainPassword = sessionService.getPasswordFromSession(token);
                
                if (plainPassword == null || plainPassword.isEmpty()) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Session expired or invalid. Please login again."));
                }
                
                log.info("Creating email for user: {} (via regular token)", tokenSubject);
            }

            // ✅ FIX: Pass password to createCustomEmail
            MailAccount mailAccount = mailboxService.createCustomEmail(user, request, plainPassword);

            // Set as primary if first email
            List<MailAccount> existing = mailboxService.getUserEmails(user.getId());
            if (existing.size() == 1) {
                mailboxService.setPrimaryEmail(user.getId(), mailAccount.getId());
                log.info("Set {} as primary email for user {}", mailAccount.getEmail(), user.getUsername());
            }

            // Prepare response
            Map<String, Object> data = new HashMap<>();
            data.put("emailId", mailAccount.getId());
            data.put("email", mailAccount.getEmail());
            data.put("emailName", mailAccount.getEmailName());
            data.put("maildirPath", mailAccount.getMaildirPath());
            data.put("isPrimary", mailAccount.getIsPrimary());
            data.put("message", "Email created successfully! You can now login with: " + mailAccount.getEmail());

            log.info("✓ Email created successfully: {}", mailAccount.getEmail());

            return ResponseEntity.ok(
                    ApiResponse.success(data, "Email created successfully"));
                    
        } catch (Exception e) {
            log.error("Failed to create email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to create email: " + e.getMessage()));
        }
    }

    /**
     * Get all emails for current user
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listEmails(
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            User user = userService.getUserByEmail(email);

            List<MailAccount> accounts = mailboxService.getUserEmails(user.getId());

            List<Map<String, Object>> emailList = accounts.stream().map(account -> {
                Map<String, Object> emailData = new HashMap<>();
                emailData.put("id", account.getId());
                emailData.put("emailName", account.getEmailName());
                emailData.put("email", account.getEmail());
                emailData.put("isPrimary", account.getIsPrimary());
                emailData.put("active", account.getActive());
                emailData.put("createdAt", account.getCreatedAt());
                emailData.put("maildirPath", account.getMaildirPath());
                return emailData;
            }).collect(Collectors.toList());

            Map<String, Object> data = new HashMap<>();
            data.put("count", emailList.size());
            data.put("emails", emailList);

            return ResponseEntity.ok(
                    ApiResponse.success(data, "Emails retrieved"));
                    
        } catch (Exception e) {
            log.error("Failed to list emails: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to retrieve emails: " + e.getMessage()));
        }
    }

    /**
     * Set primary email
     */
    @PostMapping("/{emailId}/set-primary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> setPrimary(
            @PathVariable Long emailId,
            @RequestHeader("Authorization") String authHeader) {

        try {
            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            User user = userService.getUserByEmail(email);

            mailboxService.setPrimaryEmail(user.getId(), emailId);

            MailAccount primary = mailboxService.getPrimaryEmail(user.getId());

            Map<String, Object> data = new HashMap<>();
            data.put("primaryEmail", primary.getEmail());

            log.info("✓ Primary email updated to {} for user {}", primary.getEmail(), user.getUsername());

            return ResponseEntity.ok(
                    ApiResponse.success(data, "Primary email updated"));
                    
        } catch (Exception e) {
            log.error("Failed to set primary email: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Failed to update primary email: " + e.getMessage()));
        }
    }
}