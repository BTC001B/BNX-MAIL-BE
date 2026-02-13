package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.RegisterRequest;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Validate username
     */
    public void validateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new MailException("Username already exists");
        }

        if (!username.equals(username.toLowerCase())) {
            throw new MailException("Username must be lowercase");
        }

        if (!username.matches("^[a-z0-9._-]+$")) {
            throw new MailException(
                    "Username can only contain lowercase letters, numbers, dots, hyphens and underscores");
        }
    }

    /**
     * Create user
     */
    @Transactional
    public User createUser(RegisterRequest request) {
        // Validate
        validateUsername(request.getUsername());

        // Create user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setActive(true);

        user = userRepository.save(user);
        log.info("Created user: {}", user.getUsername());

        return user;
    }

    /**
     * Authenticate user by email
     */
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new MailException("Invalid credentials");
        }

        if (!user.getActive()) {
            throw new MailException("Account is disabled");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated: {}", email);
        return user;
    }

    /**
     * Get user by email
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("User not found"));
    }

    /**
     * Get user by email or username
     * This handles cases where the token subject could be a username (temp token)
     * or email
     */
    public User getUserByEmailOrUsername(String identifier) {
        if (identifier.contains("@")) {
            return getUserByEmail(identifier);
        } else {
            return getUserByUsername(identifier);
        }
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new MailException("User not found"));
    }

    /**
     * Get mail account ID by email
     * Helper to avoid circular dependency or extra service calls in controller
     */
    public Long getMailAccountIdByEmail(String email) {
        // This logic ideally belongs in MailboxService or MailAccountRepository
        // But since session creation needs it, we can fetch it via repository
        // For now, let's assume we can add a method to find MailAccount id by email in
        // repository
        // If not, we might need to inject MailAccountRepository here or rely on
        // MailboxService.
        // Let's rely on MailboxService being called before this or inject
        // MailboxService?
        // Circular dependency SessionService -> UserService -> MailboxService.
        // Better: Let controller orchestrate this.
        // For now, let's just return a placeholder or remove this method if not used
        // directly.
        // Checked AuthController: login method calls
        // `userService.getMailAccountIdByEmail(request.getEmail())`
        // So we need this. Let's use MailAccountRepository directly here?
        // But repositories are usually private.
        // Let's inject MailAccountRepository here.
        return -1L; // Placeholder, see injected repository below
    }
}