package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.*;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
import com.btctech.mailapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final MailboxService mailboxService;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final com.btctech.mailapp.service.AuthService authService;
    private final com.btctech.mailapp.service.TwoFactorService twoFactorService;

    /**
     * STEP 1: Register user (username + password)
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request for username: {}", request.getUsername());

        // Create user
        User user = userService.createUser(request);

        // Generate temporary token (valid for email creation)
        String tempToken = jwtUtil.generateToken("temp_" + user.getUsername());

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("accountType", user.getAccountType());
        data.put("organizationId", user.getOrganization() != null ? user.getOrganization().getId() : null);
        data.put("tempToken", tempToken);
        data.put("message", "Registration successful! " + 
                (user.getAccountType().equals(com.btctech.mailapp.entity.AccountType.BUSINESS) ? 
                        "Now initialize your domain verification." : "Now create your email address."));

        return ResponseEntity.ok(
                ApiResponse.success(data, "User registered successfully"));
    }

    /**
     * Generate username suggestions based on name and DOB
     */
    @GetMapping("/username-suggestions")
    public ResponseEntity<ApiResponse<List<String>>> getUsernameSuggestions(
            @RequestParam String firstName,
            @RequestParam String lastName,
            @RequestParam String dob) {
        log.info("Generating username suggestions for {} {}, dob: {}", firstName, lastName, dob);
        List<String> suggestions = authService.generateUsernameSuggestions(firstName, lastName, dob);
        return ResponseEntity.ok(ApiResponse.success(suggestions, "Suggestions generated successfully"));
    }

    /**
     * STEP 3: Login with EMAIL + password (Enterprise Version)
     */
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        log.info("Enterprise Login request for: {}", request.getEmail());

        // 1. Authenticate & Detect Upgrade
        com.btctech.mailapp.service.UserService.LoginResult result = userService.authenticate(request.getEmail(), request.getPassword());
        User user = result.getUser();

        // 2. Extract Metadata
        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpRequest.getRemoteAddr();
        } else {
            // If there are multiple IPs (e.g., through multiple proxies), take the first one
            ipAddress = ipAddress.split(",")[0].trim();
        }

        String userAgent = httpRequest.getHeader("X-Device-Name");
        if (userAgent == null || userAgent.isEmpty()) {
            userAgent = httpRequest.getHeader("User-Agent");
        }

        // 3. Check for 2FA
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            Map<String, Object> claims = new HashMap<>();
            try {
                claims.put("ep", sessionService.encrypt(request.getPassword()));
            } catch (Exception e) {
                log.error("Failed to encrypt password for 2FA token: {}", e.getMessage());
                return ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error during login"));
            }
            String tempToken = jwtUtil.generateTokenWithClaims(claims, "2fa_" + request.getEmail());
            
            Map<String, Object> challengeData = new HashMap<>();
            challengeData.put("status", "2FA_REQUIRED");
            challengeData.put("tempToken", tempToken);
            return ResponseEntity.ok(ApiResponse.success(challengeData, "Two-factor authentication required"));
        }

        // 4. Generate Dual Tokens
        String accessToken = jwtUtil.generateToken(request.getEmail());
        String refreshToken = authService.createRefreshToken(user, ipAddress, userAgent);

        // 5. Get primary mail account for session
        MailAccount mailAccount = mailboxService.getMailAccountByEmail(request.getEmail());

        // 6. Create session (store password linked to accessToken)
        sessionService.createSession(user.getId(), mailAccount.getId(),
                request.getPassword(), accessToken);

        // 7. BACKFILL: Ensure MailAccount has an encrypted_password for Always-On sending
        if (mailAccount.getEncryptedPassword() == null) {
            try {
                mailAccount.setEncryptedPassword(sessionService.encrypt(request.getPassword()));
                mailboxService.saveMailAccount(mailAccount);
                log.info("✓ Backfilled encrypted SMTP password for: {}", mailAccount.getEmail());
            } catch (Exception e) {
                log.error("Failed to backfill encrypted password: {}", e.getMessage());
            }
        }

        // 7. Build Rich SaaS Response
        com.btctech.mailapp.dto.LoginResponseData data = authService.buildLoginResponse(user, result.isAutoUpgraded(), accessToken, refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Login successful"));
    }

    /**
     * Final STEP: Verify 2FA code and complete login
     */
    @PostMapping("/login/2fa")
    public ResponseEntity<?> verifyLogin2fa(@RequestBody Map<String, String> body, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String tempToken = body.get("tempToken");
        String code = body.get("code");
        String subject = jwtUtil.extractEmail(tempToken);
        String email = subject.replace("2fa_", "");
        User user = userService.getUserByEmailOrUsername(email);
        if (twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            // Generate real tokens
            String ipAddress = httpRequest.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = httpRequest.getRemoteAddr();
            } else {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            String userAgent = httpRequest.getHeader("User-Agent");
            
            String accessToken = jwtUtil.generateToken(email);
            String refreshToken = authService.createRefreshToken(user, ipAddress, userAgent);
            
            try {
                // Recover the password from the tempToken claims
                io.jsonwebtoken.Claims claims = jwtUtil.extractAllClaims(tempToken);
                String encryptedPassword = claims.get("ep", String.class);
                if (encryptedPassword != null) {
                    String password = sessionService.decrypt(encryptedPassword);
                    // Get primary mail account for session
                    MailAccount mailAccount = mailboxService.getMailAccountByEmail(email);
                    // Create session (store password linked to accessToken)
                    sessionService.createSession(user.getId(), mailAccount.getId(), password, accessToken);
                }
            } catch (Exception e) {
                log.error("Failed to recover password from 2FA token: {}", e.getMessage());
            }
            
            com.btctech.mailapp.dto.LoginResponseData data = authService.buildLoginResponse(user, false, accessToken, refreshToken);
            return ResponseEntity.ok(ApiResponse.success(data, "Login successful"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid 2FA code"));
        }
    }

    /**
     * Get all active sessions for current user
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<java.util.List<com.btctech.mailapp.dto.SessionResponse>>> getSessions(
            @RequestHeader("Authorization") String authHeader,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        String ipAddress = httpRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = httpRequest.getRemoteAddr();
        } else {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Fetching sessions for user: {}", user.getUsername());
        java.util.List<com.btctech.mailapp.dto.SessionResponse> sessions = authService.getActiveSessions(user, ipAddress, userAgent);

        return ResponseEntity.ok(
                ApiResponse.success(sessions, "Sessions retrieved successfully"));
    }

    /**
     * Remotely revoke a specific session
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        log.info("Revoking session {} for user {}", sessionId, user.getUsername());
        authService.revokeSession(sessionId, user);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Session revoked successfully"));
    }

    /**
     * Token Rotation: Get new access token using refresh token
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<com.btctech.mailapp.dto.LoginResponseData>> refresh(
            @Valid @RequestBody com.btctech.mailapp.dto.TokenRefreshRequest request) {
        
        log.info("Token rotation request");
        com.btctech.mailapp.dto.LoginResponseData data = authService.refreshToken(request);
        
        return ResponseEntity.ok(
                ApiResponse.success(data, "Token refreshed successfully"));
    }

    /**
     * Logout: Revoke tokens and cleanup session
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody com.btctech.mailapp.dto.TokenRefreshRequest request) {
        
        String accessToken = authHeader.substring(7);
        String refreshToken = request.getRefreshToken();

        log.info("Logout request for session: {}", accessToken);

        // 1. Revoke refresh token in DB
        authService.logout(refreshToken);

        // 2. Cleanup password session
        sessionService.deleteSession(accessToken);

        return ResponseEntity.ok(
                ApiResponse.success(null, "Logged out successfully"));
    }

    /**
     * Change Password: Update user and mail account passwords
     */
    @PostMapping("/change-password")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        log.info("Password change request for user: {}", user.getUsername());

        // 1. Validate old password
        userService.authenticate(email, request.getOldPassword());

        // 2. Perform atomic update
        userService.updateUserPassword(user, request.getNewPassword());

        return ResponseEntity.ok(
                ApiResponse.success(null, "Password changed successfully"));
    }

    @PostMapping("/child/send-parent-otp")
    public ResponseEntity<ApiResponse<Void>> sendParentOtp(@RequestBody java.util.Map<String, String> request) {
        String parentEmail = request.get("parentEmail");
        if (parentEmail == null || parentEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Parent email is required"));
        }
        authService.sendParentOtp(parentEmail);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent to parent email"));
    }

    @PostMapping("/child/verify-parent-otp")
    public ResponseEntity<ApiResponse<Void>> verifyParentOtp(@RequestBody java.util.Map<String, String> request) {
        String parentEmail = request.get("parentEmail");
        String otp = request.get("otp");
        if (parentEmail == null || parentEmail.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Parent email and OTP are required"));
        }
        authService.verifyParentOtp(parentEmail, otp);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully"));
    }

    /**
     * Get masked recovery options for forgot password
     */
    @GetMapping("/forgot-password/options")
    public ResponseEntity<ApiResponse<RecoveryOptionsResponse>> getRecoveryOptions(
            @RequestParam String identifier) {
        log.info("Fetching recovery options for: {}", identifier);
        RecoveryOptionsResponse options = authService.getRecoveryOptions(identifier);
        return ResponseEntity.ok(ApiResponse.success(options, "Recovery options retrieved successfully"));
    }

    /**
     * Send OTP to selected recovery method
     */
    @PostMapping("/forgot-password/send-otp")
    public ResponseEntity<ApiResponse<Void>> sendRecoveryOtp(
            @Valid @RequestBody SendOtpRequest request) {
        log.info("Sending recovery OTP to {} via {}", request.getIdentifier(), request.getMethod());
        authService.sendRecoveryOtp(request);
        return ResponseEntity.ok(ApiResponse.success(null, "OTP sent successfully"));
    }

    /**
     * Verify OTP without resetting password
     */
    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyRecoveryOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verifying OTP for: {}", request.getIdentifier());
        authService.verifyOtp(request.getIdentifier(), request.getOtp());
        return ResponseEntity.ok(ApiResponse.success(null, "OTP verified successfully"));
    }

    /**
     * Reset password using OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Resetting password for: {}", request.getIdentifier());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }

    /**
     * Get all external application sessions (SSO)
     */
    @GetMapping("/sessions/external")
    public ResponseEntity<ApiResponse<java.util.List<com.btctech.mailapp.dto.ExternalSessionResponse>>> getExternalSessions(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        log.info("Fetching external sessions for user: {}", user.getUsername());
        java.util.List<com.btctech.mailapp.dto.ExternalSessionResponse> sessions = authService.getExternalSessions(user);

        return ResponseEntity.ok(
                ApiResponse.success(sessions, "External sessions retrieved successfully"));
    }

    /**
     * Remotely revoke a specific external app session
     */
    @DeleteMapping("/sessions/external/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeExternalSession(
            @PathVariable Long sessionId,
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        log.info("Revoking external session {} for user {}", sessionId, user.getUsername());
        authService.revokeExternalSession(sessionId, user);

        return ResponseEntity.ok(
                ApiResponse.success(null, "External app access revoked successfully"));
    }

    /**
     * Recovery Path: Send OTP to email when 2FA device is lost
     */
    @PostMapping("/login/2fa/send-otp")
    public ResponseEntity<ApiResponse<Map<String, String>>> send2faRecoveryOtp(
            @RequestBody Map<String, String> request) {
        
        String tempToken = request.get("tempToken");
        if (tempToken == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing temporary token"));
        }

        try {
            String subject = jwtUtil.extractEmail(tempToken);
            String email = subject.startsWith("2fa_") ? subject.substring(4) : subject;
            
            // Re-use SendOtpRequest DTO logic
            com.btctech.mailapp.dto.SendOtpRequest otpRequest = new com.btctech.mailapp.dto.SendOtpRequest();
            otpRequest.setIdentifier(email);
            otpRequest.setMethod("EMAIL");
            
            authService.sendRecoveryOtp(otpRequest);
            
            log.info("✓ 2FA recovery OTP sent to {}", email);
            return ResponseEntity.ok(ApiResponse.success(null, "Recovery code sent to your email"));
            
        } catch (Exception e) {
            log.error("Failed to send 2FA recovery OTP: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to send recovery code: " + e.getMessage()));
        }
    }

    /**
     * Recovery Path: Verify OTP and login
     */
    @PostMapping("/login/2fa/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponseData>> verify2faRecoveryOtp(
            @RequestBody Map<String, String> request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        String tempToken = request.get("tempToken");
        String otp = request.get("otp");
        
        if (tempToken == null || otp == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Missing token or OTP"));
        }

        try {
            String subject = jwtUtil.extractEmail(tempToken);
            String email = subject.startsWith("2fa_") ? subject.substring(4) : subject;
            
            // 1. Verify OTP
            authService.verifyOtp(email, otp);
            
            // 2. OTP is valid! Proceed with login (Bypass 2FA)
            User user = userService.getUserByEmailOrUsername(email);
            
            // Standard login metadata extraction
            String ipAddress = httpRequest.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isEmpty()) {
                ipAddress = httpRequest.getRemoteAddr();
            } else {
                ipAddress = ipAddress.split(",")[0].trim();
            }
            String userAgent = httpRequest.getHeader("User-Agent");

            // Generate Dual Tokens
            String accessToken = jwtUtil.generateToken(email);
            String refreshToken = authService.createRefreshToken(user, ipAddress, userAgent);

            // Create session
            MailAccount mailAccount = mailboxService.getMailAccountByEmail(email);
            
            // Use persistent password if available (Always-On)
            String password = null;
            if (mailAccount.getEncryptedPassword() != null) {
                password = sessionService.decrypt(mailAccount.getEncryptedPassword());
            }
            
            if (password != null) {
                sessionService.createSession(user.getId(), mailAccount.getId(), password, accessToken);
            }

            LoginResponseData data = authService.buildLoginResponse(user, false, accessToken, refreshToken);
            log.info("✓ User {} logged in via 2FA Email Recovery", email);
            
            return ResponseEntity.ok(ApiResponse.success(data, "Login successful via recovery"));
            
        } catch (Exception e) {
            log.error("2FA OTP verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error("Verification failed: " + e.getMessage()));
        }
    }
}
