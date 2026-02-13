package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.UserSession;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {
    
    private final UserSessionRepository sessionRepository;
    
    @Value("${encryption.key}")
    private String encryptionKey;
    
    /**
     * Create session with encrypted password
     * Each user gets their own session
     */
    @Transactional
    public UserSession createSession(Long userId, Long mailAccountId, 
                                     String password, String jwtToken) {
        try {
            // Check if session already exists for this token
            sessionRepository.findByJwtToken(jwtToken).ifPresent(existing -> {
                log.info("Removing existing session for token");
                sessionRepository.delete(existing);
            });
            
            // Encrypt password
            String encryptedPassword = encrypt(password);
            
            // Create new session
            UserSession session = new UserSession();
            session.setUserId(userId);
            session.setMailAccountId(mailAccountId);
            session.setEncryptedPassword(encryptedPassword);
            session.setJwtToken(jwtToken);
            session.setExpiresAt(LocalDateTime.now().plusDays(1)); // 24 hours
            
            session = sessionRepository.save(session);
            log.info("Created session for user: {} (mail_account: {})", userId, mailAccountId);
            
            return session;
            
        } catch (Exception e) {
            log.error("Failed to create session: {}", e.getMessage(), e);
            throw new MailException("Failed to create session");
        }
    }
    
    /**
     * Get password from session by JWT token
     * Each user's password is isolated by their token
     */
    public String getPasswordFromSession(String jwtToken) {
        try {
            UserSession session = sessionRepository.findByJwtToken(jwtToken)
                    .orElseThrow(() -> new MailException("Session not found. Please login again."));
            
            // Check expiry
            if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
                log.warn("Session expired for user: {}", session.getUserId());
                sessionRepository.delete(session);
                throw new MailException("Session expired. Please login again.");
            }
            
            // Decrypt password
            String password = decrypt(session.getEncryptedPassword());
            log.debug("Retrieved password for user: {}", session.getUserId());
            
            return password;
            
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get password from session: {}", e.getMessage(), e);
            throw new MailException("Failed to retrieve session. Please login again.");
        }
    }
    
    /**
     * Cleanup expired sessions
     */
    @Transactional
    public void cleanupExpiredSessions() {
        int deleted = sessionRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired sessions", deleted);
        }
    }
    
    /**
     * Delete session by token (logout)
     */
    @Transactional
    public void deleteSession(String jwtToken) {
        sessionRepository.findByJwtToken(jwtToken).ifPresent(session -> {
            sessionRepository.delete(session);
            log.info("Deleted session for user: {}", session.getUserId());
        });
    }
    
    /**
     * Encrypt password using AES
     */
    private String encrypt(String plainText) throws Exception {
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(plainText.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    /**
     * Decrypt password using AES
     */
    private String decrypt(String encryptedText) throws Exception {
        SecretKeySpec key = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, key);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted);
    }
}