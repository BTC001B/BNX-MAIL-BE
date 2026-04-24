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
}
