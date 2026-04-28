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

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final MailboxService mailboxService;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;
    private final com.btctech.mailapp.service.AuthService authService; // ✅ Inject AuthService

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
     * STEP 3: Login with EMAIL + password (Enterprise Version)
     */
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<com.btctech.mailapp.dto.LoginResponseData>> login(
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

        // 3. Generate Dual Tokens
        String accessToken = jwtUtil.generateToken(request.getEmail());
        String refreshToken = authService.createRefreshToken(user, ipAddress, userAgent);

        // 4. Get primary mail account for session
        MailAccount mailAccount = mailboxService.getMailAccountByEmail(request.getEmail());

        // 5. Create session (store password linked to accessToken)
        sessionService.createSession(user.getId(), mailAccount.getId(),
                request.getPassword(), accessToken);

        // 6. Build Rich SaaS Response
        com.btctech.mailapp.dto.LoginResponseData data = authService.buildLoginResponse(user, result.isAutoUpgraded(), accessToken, refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(data, "Login successful"));
    }

    /**
     * Get all active sessions for current user
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<java.util.List<com.btctech.mailapp.dto.SessionResponse>>> getSessions(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmail(email);

        log.info("Fetching sessions for user: {}", user.getUsername());
        java.util.List<com.btctech.mailapp.dto.SessionResponse> sessions = authService.getActiveSessions(user, null); // We don't track current access token here yet

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
     * Reset password using OTP
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Resetting password for: {}", request.getIdentifier());
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Password reset successfully"));
    }
}
