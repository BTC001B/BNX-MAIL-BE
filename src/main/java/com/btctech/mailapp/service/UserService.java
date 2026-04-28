package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.AccountType;
import com.btctech.mailapp.dto.RegisterRequest;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.UserSettings;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.repository.MailAccountRepository;
import com.btctech.mailapp.repository.UserSettingsRepository;
import com.btctech.mailapp.entity.MailAccount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.btctech.mailapp.strategy.RegistrationStrategy;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailAccountRepository mailAccountRepository;
    private final UserSettingsRepository userSettingsRepository;
    private final com.btctech.mailapp.repository.ActivityLogRepository activityLogRepository;
    private final Map<String, RegistrationStrategy> registrationStrategies;

    @Autowired
    public UserService(UserRepository userRepository, 
                       PasswordEncoder passwordEncoder,
                       MailAccountRepository mailAccountRepository,
                       UserSettingsRepository userSettingsRepository,
                       com.btctech.mailapp.repository.ActivityLogRepository activityLogRepository,
                       List<RegistrationStrategy> strategies) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailAccountRepository = mailAccountRepository;
        this.userSettingsRepository = userSettingsRepository;
        this.activityLogRepository = activityLogRepository;
        this.registrationStrategies = strategies.stream()
                .collect(Collectors.toMap(RegistrationStrategy::getMode, Function.identity()));
    }

    /**
     * Get or create default user settings
     */
    @Transactional
    public UserSettings getSettings(User user) {
        return userSettingsRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    try {
                        log.info("Creating new default settings for user: {}", user.getEmail());
                        UserSettings settings = UserSettings.builder()
                                .user(user)
                                .inboxNotifications(true)
                                .sentNotifications(false)
                                .starredNotifications(true)
                                .snoozedNotifications(true)
                                .soundEnabled(true)
                                .vibrationEnabled(true)
                                .themeMode("System Default")
                                .accentColor("#4F46E5")
                                .fontSize(1.0)
                                .density("Default")
                                .storageLimit(16106127360L)
                                .build();
                        return userSettingsRepository.save(settings);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        log.warn("Settings already exist for user {} (race condition handled)", user.getEmail());
                        return userSettingsRepository.findByUserId(user.getId())
                            .orElseThrow(() -> new MailException("Failed to retrieve or create user settings"));
                    }
                });
    }

    /**
     * Update user settings
     */
    @Transactional
    public UserSettings updateSettings(User user, UserSettings newSettings) {
        UserSettings existing = getSettings(user);
        
        // Update only non-null fields or specific ones
        if (newSettings.getPhoneNumber() != null) existing.setPhoneNumber(newSettings.getPhoneNumber());
        if (newSettings.getLocation() != null) existing.setLocation(newSettings.getLocation());
        if (newSettings.getJobTitle() != null) existing.setJobTitle(newSettings.getJobTitle());
        
        if (newSettings.getInboxNotifications() != null) existing.setInboxNotifications(newSettings.getInboxNotifications());
        if (newSettings.getSentNotifications() != null) existing.setSentNotifications(newSettings.getSentNotifications());
        if (newSettings.getStarredNotifications() != null) existing.setStarredNotifications(newSettings.getStarredNotifications());
        if (newSettings.getSnoozedNotifications() != null) existing.setSnoozedNotifications(newSettings.getSnoozedNotifications());
        if (newSettings.getSoundEnabled() != null) existing.setSoundEnabled(newSettings.getSoundEnabled());
        if (newSettings.getVibrationEnabled() != null) existing.setVibrationEnabled(newSettings.getVibrationEnabled());
        if (newSettings.getQuietHoursEnabled() != null) existing.setQuietHoursEnabled(newSettings.getQuietHoursEnabled());
        if (newSettings.getQuietHoursStart() != null) existing.setQuietHoursStart(newSettings.getQuietHoursStart());
        if (newSettings.getQuietHoursEnd() != null) existing.setQuietHoursEnd(newSettings.getQuietHoursEnd());
        
        if (newSettings.getThemeMode() != null) existing.setThemeMode(newSettings.getThemeMode());
        if (newSettings.getAccentColor() != null) existing.setAccentColor(newSettings.getAccentColor());
        if (newSettings.getFontSize() != null) existing.setFontSize(newSettings.getFontSize());
        if (newSettings.getDensity() != null) existing.setDensity(newSettings.getDensity());
        
        if (newSettings.getTwoFactorEnabled() != null) existing.setTwoFactorEnabled(newSettings.getTwoFactorEnabled());
        if (newSettings.getBiometricsEnabled() != null) existing.setBiometricsEnabled(newSettings.getBiometricsEnabled());
        if (newSettings.getLanguage() != null) existing.setLanguage(newSettings.getLanguage());
        
        return userSettingsRepository.save(existing);
    }

    /**
     * Log user activity
     */
    @Transactional
    public void logActivity(User user, String activity, String details, String ipAddress, String deviceName) {
        com.btctech.mailapp.entity.ActivityLog logEntry = com.btctech.mailapp.entity.ActivityLog.builder()
                .user(user)
                .activity(activity)
                .details(details)
                .ipAddress(ipAddress)
                .deviceName(deviceName)
                .build();
        activityLogRepository.save(logEntry);
        log.info("Activity logged for {}: {}", user.getUsername(), activity);
    }

    public List<com.btctech.mailapp.entity.ActivityLog> getActivityLogs(User user) {
        return activityLogRepository.findTop20ByUserIdOrderByTimestampDesc(user.getId());
    }

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
     * Create user using strategies
     */
    @Transactional
    public User createUser(RegisterRequest request) {
        validateUsername(request.getUsername());

        RegistrationStrategy strategy = registrationStrategies.get(request.getMode().toUpperCase());
        if (strategy == null) {
            throw new MailException("Unsupported registration mode: " + request.getMode());
        }

        return strategy.register(request);
    }

    /**
     * Auth Result DTO
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class LoginResult {
        private User user;
        private boolean autoUpgraded;
    }

    /**
     * Authenticate user by email
     */
    public LoginResult authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new MailException("Invalid credentials");
        }

        if (!user.getActive()) {
            throw new MailException("Account is disabled");
        }

        boolean autoUpgraded = false;

        // AUTO-UPGRADE: CHILD to PUBLIC if 18+
        if (user.getAccountType() == AccountType.CHILD && user.getDob() != null) {
            int age = Period.between(user.getDob(), LocalDate.now()).getYears();
            if (age >= 18) {
                log.info("Auto-upgrading user {} from CHILD to PUBLIC (Age: {})", user.getUsername(), age);
                user.setAccountType(AccountType.PUBLIC);
                user.setRole("PUBLIC_USER");
                user.setApproved(true);
                autoUpgraded = true;
            }
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated: {}", email);
        return new LoginResult(user, autoUpgraded);
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
            try {
                return getUserByEmail(identifier);
            } catch (MailException e) {
                // FALLBACK: Check if this email belongs to any MailAccount
                return mailAccountRepository.findByEmail(identifier)
                        .map(ma -> userRepository.findById(ma.getUserId())
                                .orElseThrow(() -> new MailException("User not found for this mail account")))
                        .orElseThrow(() -> new MailException("User not found: " + identifier));
            }
        } else {
            return getUserByUsername(identifier);
        }
    }

    /**
     * ATOMIC: Update both User and Mail Account passwords
     */
    @Transactional
    public void updateUserPassword(User user, String newPlainPassword) {
        log.info("Performing atomic password update for user: {}", user.getUsername());
        
        // 1. Update User password (Hashed for Spring Auth)
        String userHash = passwordEncoder.encode(newPlainPassword);
        user.setPassword(userHash);
        userRepository.save(user);
        
        // 2. Update all associated MailAccount passwords (Hashed with {BLF-CRYPT} for Dovecot)
        String dovecotHash = "{BLF-CRYPT}" + userHash; // BCrypt hash is the same, just prefix it
        
        List<MailAccount> accounts = mailAccountRepository.findByUserId(user.getId());
        for (MailAccount account : accounts) {
            log.debug("Updating mail account: {}", account.getEmail());
            account.setPassword(dovecotHash);
            mailAccountRepository.save(account);
        }
        
        log.info("✓ Atomic password update complete for {} ({} mailboxes updated)", 
            user.getUsername(), accounts.size());
    }

    /**
     * Get user by username
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new MailException("User not found"));
    }

    /**
     * Approve a child account (Lifecycle Unlock)
     */
    @Transactional
    public User approveChild(User parent, Long childId) {
        // 1. Fetch child
        User child = userRepository.findById(childId)
                .orElseThrow(() -> new MailException("Child user not found"));

        if (!AccountType.CHILD.equals(child.getAccountType())) {
            throw new MailException("Target user is not a CHILD account");
        }

        // 2. Security Check: Parent ownership
        if (child.getParent() == null || !child.getParent().getId().equals(parent.getId())) {
            log.warn("Unauthorized approval attempt by user {} for child {}", parent.getUsername(), childId);
            throw new MailException("Unauthorized: You can only approve your own child accounts");
        }

        if (Boolean.TRUE.equals(child.getApproved())) {
            throw new MailException("Account is already approved");
        }

        // 3. Final Approval & Audit
        child.setApproved(true);
        child.setApprovedBy(parent.getId());
        child.setApprovedAt(LocalDateTime.now());
        
        User savedChild = userRepository.save(child);
        
        log.info("✓ CHILD account approved: {} by parent: {}", savedChild.getUsername(), parent.getUsername());
        
        return savedChild;
    }
}