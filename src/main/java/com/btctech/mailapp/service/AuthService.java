package com.btctech.mailapp.service;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.LoginResponseData;
import com.btctech.mailapp.dto.TokenRefreshRequest;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.RefreshToken;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import com.btctech.mailapp.dto.RecoveryOptionsResponse;
import com.btctech.mailapp.dto.SendOtpRequest;
import com.btctech.mailapp.dto.ResetPasswordRequest;
import com.btctech.mailapp.entity.PasswordResetToken;
import com.btctech.mailapp.repository.PasswordResetTokenRepository;
import java.security.SecureRandom;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final MailboxService mailboxService;
    private final SessionService sessionService;
    private final UserService userService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender javaMailSender;

    /**
     * Create and persist a refresh token with metadata
     */
    public String createRefreshToken(User user, String ipAddress, String userAgent) {
        // Build the actual JWT for refresh
        String tokenStr = jwtUtil.generateRefreshToken(user.getEmail() != null ? user.getEmail() : user.getUsername());
        
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenStr)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiryDate(Instant.now().plusMillis(604800000)) // 7 days
                .build();

        refreshTokenRepository.save(refreshToken);
        return tokenStr;
    }

    /**
     * Get all active sessions for current user
     */
    public java.util.List<com.btctech.mailapp.dto.SessionResponse> getActiveSessions(User user, String currentToken) {
        return refreshTokenRepository.findAllByUserAndRevokedFalse(user).stream()
                .filter(token -> !token.isExpired())
                .map(token -> com.btctech.mailapp.dto.SessionResponse.builder()
                        .id(token.getId())
                        .ipAddress(token.getIpAddress())
                        .userAgent(token.getUserAgent())
                        .createdAt(token.getCreatedAt())
                        .expiresAt(token.getExpiryDate())
                        .isCurrentSession(token.getToken().equals(currentToken))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Remotely revoke a specific session
     */
    @Transactional
    public void revokeSession(Long sessionId, User user) {
        RefreshToken token = refreshTokenRepository.findById(sessionId)
                .orElseThrow(() -> new MailException("Session not found"));

        if (!token.getUser().getId().equals(user.getId())) {
            throw new MailException("Unauthorized to revoke this session");
        }

        token.setRevoked(true);
        refreshTokenRepository.save(token);
        log.info("✓ Session {} revoked for user {}", sessionId, user.getUsername());
    }

    /**
     * Rotate or refresh access token using refresh token
     */
    @Transactional
    public LoginResponseData refreshToken(TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenRepository.findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    String accessToken = jwtUtil.generateToken(user.getEmail());
                    
                    // ✅ FIX: Migrate password session to new access token
                    try {
                        String password = sessionService.getPasswordByUserId(user.getId());
                        if (password != null) {
                            MailAccount primaryAccount = mailboxService.getPrimaryEmail(user.getId());
                            sessionService.createSession(user.getId(), primaryAccount.getId(), password, accessToken);
                            log.info("✓ Migrated session to new access token for user: {}", user.getUsername());
                        }
                    } catch (Exception e) {
                        log.warn("⚠ Could not migrate password session during token refresh: {}", e.getMessage());
                    }

                    return buildLoginResponse(user, false, accessToken, requestRefreshToken);
                })
                .orElseThrow(() -> new MailException("Refresh token is not in database!"));
    }

    private RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired() || token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw new MailException("Refresh token was expired or revoked. Please log in again.");
        }
        return token;
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    /**
     * Build the complex SaaS login response
     */
    public LoginResponseData buildLoginResponse(User user, boolean autoUpgraded, String accessToken, String refreshToken) {
        List<MailAccount> userMailboxes = mailboxService.getUserEmails(user.getId());
        
        List<LoginResponseData.MailboxSummary> boxSummaries = userMailboxes.stream()
                .map(box -> LoginResponseData.MailboxSummary.builder()
                        .emailId(box.getId())
                        .email(box.getEmail())
                        .isPrimary(box.getIsPrimary())
                        .build())
                .collect(Collectors.toList());

        return LoginResponseData.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole())
                .accountType(user.getAccountType().name())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(jwtUtil.getExpirationSeconds(accessToken))
                .refreshTokenExpiresIn(jwtUtil.getExpirationSeconds(refreshToken))
                .mailboxes(boxSummaries)
                .isAutoUpgraded(autoUpgraded)
                .loginAt(LocalDateTime.now())
                .build();
    }

    /**
     * Get masked recovery options for the user
     */
    public RecoveryOptionsResponse getRecoveryOptions(String identifier) {
        User user = userService.getUserByEmailOrUsername(identifier);

        return RecoveryOptionsResponse.builder()
                .recoveryEmail(maskEmail(user.getRecoveryEmail()))
                .phoneNumber(maskPhone(user.getPhoneNumber()))
                .build();
    }

    /**
     * Generate and send OTP to the selected recovery method
     */
    @Transactional
    public void sendRecoveryOtp(SendOtpRequest request) {
        User user = userService.getUserByEmailOrUsername(request.getIdentifier());

        if ("EMAIL".equalsIgnoreCase(request.getMethod()) && user.getRecoveryEmail() == null) {
            throw new MailException("No recovery email configured for this account");
        } else if ("PHONE".equalsIgnoreCase(request.getMethod()) && user.getPhoneNumber() == null) {
            throw new MailException("No recovery phone configured for this account");
        }

        // Clean up old tokens
        passwordResetTokenRepository.deleteByUser(user);

        // Generate 6-digit OTP
        SecureRandom random = new SecureRandom();
        int otpValue = 100000 + random.nextInt(900000);
        String otp = String.valueOf(otpValue);

        // Save token
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(otp)
                .expiryDate(LocalDateTime.now().plusMinutes(15))
                .build();
        passwordResetTokenRepository.save(resetToken);

        if ("EMAIL".equalsIgnoreCase(request.getMethod())) {
            sendOtpEmail(user.getRecoveryEmail(), otp);
        } else if ("PHONE".equalsIgnoreCase(request.getMethod())) {
            // Mock SMS sending
            log.info("Mock SMS sent to {}: Your BNX Mail password reset OTP is {}", user.getPhoneNumber(), otp);
        } else {
            throw new MailException("Invalid recovery method");
        }
    }

    private void sendOtpEmail(String toAddress, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("beta@bnxmail.com");
            message.setTo(toAddress);
            message.setSubject("Your BNX Mail Password Reset OTP");
            message.setText("Your OTP for password reset is: " + otp + "\nThis code will expire in 15 minutes.");
            javaMailSender.send(message);
            log.info("Sent recovery OTP email to {}", toAddress);
        } catch (Exception e) {
            log.error("Failed to send recovery email: {}", e.getMessage());
            throw new MailException("Failed to send recovery email. Please try again later.");
        }
    }

    /**
     * Validate OTP and reset password
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userService.getUserByEmailOrUsername(request.getIdentifier());

        PasswordResetToken token = passwordResetTokenRepository.findByUserAndToken(user, request.getOtp())
                .orElseThrow(() -> new MailException("Invalid OTP"));

        if (token.isExpired()) {
            passwordResetTokenRepository.delete(token);
            throw new MailException("OTP has expired");
        }

        // Perform password update atomic flow
        userService.updateUserPassword(user, request.getNewPassword());

        // Delete used token
        passwordResetTokenRepository.delete(token);

        // Optionally, revoke all active sessions so they have to log in with new password
        List<RefreshToken> activeTokens = refreshTokenRepository.findAllByUserAndRevokedFalse(user);
        for (RefreshToken rt : activeTokens) {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        }
        log.info("Revoked all active sessions for user {} after password reset", user.getUsername());
    }

    // Helper functions for masking
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return null;
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 2) {
            return name + "***@" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + "@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return null;
        return "*******" + phone.substring(phone.length() - 3);
    }
}
