package com.btctech.mailapp.service;

import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.repository.MailAccountRepository;
import com.btctech.mailapp.repository.RefreshTokenRepository;
import com.btctech.mailapp.repository.OrganizationRepository;
import com.btctech.mailapp.repository.BusinessProfileRepository;
import com.btctech.mailapp.repository.DomainRepository;
import com.btctech.mailapp.repository.ActivityLogRepository;
import com.btctech.mailapp.repository.SystemSettingRepository;
import com.btctech.mailapp.entity.ActivityLog;
import com.btctech.mailapp.entity.SystemSetting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.MailAccount;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final MailAccountRepository mailAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OrganizationRepository organizationRepository;
    private final BusinessProfileRepository businessProfileRepository;
    private final DomainRepository domainRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final com.btctech.mailapp.repository.ReportRepository reportRepository;
    private final com.btctech.mailapp.repository.AppealRepository appealRepository;
    private final JavaMailSender mailSender;
    private final SessionService sessionService;
    private final com.btctech.mailapp.repository.UserSessionRepository userSessionRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final UserService userService;

    public void sendGlobalBroadcast(String adminUsername, String subject, String message) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        List<User> activeUsers = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getActive()))
                .collect(Collectors.toList());
                
        for (User user : activeUsers) {
            String toEmail = user.getEmail() != null ? user.getEmail() : user.getUsername() + "@bnxmail.com";
            try {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setFrom("admin@bnxmail.com");
                mailMessage.setTo(toEmail);
                mailMessage.setSubject(subject);
                mailMessage.setText(message);
                mailSender.send(mailMessage);
            } catch (Exception e) {
                log.error("Failed to send broadcast to {}", toEmail, e);
            }
        }
        
        if (admin != null) {
            ActivityLog logRecord = new ActivityLog();
            logRecord.setUser(admin);
            logRecord.setActivity("Global Broadcast Sent");
            logRecord.setDetails("Subject: " + subject + " to " + activeUsers.size() + " users.");
            activityLogRepository.save(logRecord);
        }
    }

    @Transactional
    public void forceGlobalLogoutAll(String adminUsername) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        refreshTokenRepository.deleteAll();
        userSessionRepository.deleteAll();
        
        if (admin != null) {
            ActivityLog logRecord = new ActivityLog();
            logRecord.setUser(admin);
            logRecord.setActivity("Forced Global Logout ALL Users");
            logRecord.setDetails("All sessions globally destroyed by admin.");
            activityLogRepository.save(logRecord);
        }
    }

    public Map<String, String> getSystemSettings() {
        return systemSettingRepository.findAll().stream()
                .collect(Collectors.toMap(SystemSetting::getSettingKey, SystemSetting::getSettingValue));
    }

    @Transactional
    public void updateSystemSettings(String adminUsername, Map<String, String> newSettings) {
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        
        for (Map.Entry<String, String> entry : newSettings.entrySet()) {
            SystemSetting setting = systemSettingRepository.findById(entry.getKey())
                    .orElse(new SystemSetting(entry.getKey(), ""));
            setting.setSettingValue(entry.getValue());
            systemSettingRepository.save(setting);
        }

        if (admin != null) {
            ActivityLog logRecord = new ActivityLog();
            logRecord.setUser(admin);
            logRecord.setActivity("System Settings Updated");
            logRecord.setDetails("Updated keys: " + String.join(", ", newSettings.keySet()));
            activityLogRepository.save(logRecord);
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getAbuseCase(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        List<com.btctech.mailapp.entity.Report> reports = reportRepository.findByReportedUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> reportList = reports.stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("reporterEmail", r.getReporter().getEmail() != null ? r.getReporter().getEmail() : r.getReporter().getUsername() + "@bnxmail.com");
            map.put("reason", r.getReason());
            map.put("emailSubject", r.getReportedEmailSubject());
            map.put("date", r.getCreatedAt());
            return map;
        }).collect(Collectors.toList());

        java.util.Optional<com.btctech.mailapp.entity.Appeal> appealOpt = appealRepository.findFirstByBannedUserIdOrderByCreatedAtDesc(userId);
        
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("reportedUserEmail", user.getEmail() != null ? user.getEmail() : user.getUsername() + "@bnxmail.com");
        caseData.put("reportedUserName", user.getFirstName() + " " + user.getLastName());
        caseData.put("reports", reportList);
        caseData.put("appeal", appealOpt.map(com.btctech.mailapp.entity.Appeal::getAppealMessage).orElse(null));
        
        return caseData;
    }
    
    @Transactional
    public void decideAbuseCase(Long userId, String decision) {
        User user = userRepository.findById(userId).orElseThrow();
        if ("UNBAN".equalsIgnoreCase(decision)) {
            user.setActive(true);
            userRepository.save(user);
            reportRepository.deleteByReportedUserId(userId);
            
            // Optionally mark appeal as APPROVED
            appealRepository.findFirstByBannedUserIdOrderByCreatedAtDesc(userId).ifPresent(appeal -> {
                appeal.setStatus(com.btctech.mailapp.entity.Appeal.AppealStatus.APPROVED);
                appealRepository.save(appeal);
            });
            
            ActivityLog logRecord = new ActivityLog();
            logRecord.setUser(user);
            logRecord.setActivity("Unbanned with Warning by Admin");
            activityLogRepository.save(logRecord);
        } else if ("BAN".equalsIgnoreCase(decision)) {
            appealRepository.findFirstByBannedUserIdOrderByCreatedAtDesc(userId).ifPresent(appeal -> {
                appeal.setStatus(com.btctech.mailapp.entity.Appeal.AppealStatus.REJECTED);
                appealRepository.save(appeal);
            });
            ActivityLog logRecord = new ActivityLog();
            logRecord.setUser(user);
            logRecord.setActivity("Appeal Rejected - Permanent Ban enforced");
            activityLogRepository.save(logRecord);
        }
    }

    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            long totalUsers = userRepository.count();
            long activeMailAccounts = mailAccountRepository.count();
            long activeSessions = refreshTokenRepository.count();
            long totalOrganizations = organizationRepository.count();
            long totalBusinessProfiles = businessProfileRepository.count();
            long totalDomains = domainRepository.count();
            
            Long totalStorageBytes = mailAccountRepository.calculateTotalStorageUsed();
            if (totalStorageBytes == null) {
                totalStorageBytes = 0L;
            }
            
            metrics.put("totalUsers", totalUsers);
            metrics.put("activeMailAccounts", activeMailAccounts);
            metrics.put("activeSessions", activeSessions);
            metrics.put("totalStorageBytes", totalStorageBytes);
            metrics.put("totalOrganizations", totalOrganizations);
            metrics.put("totalBusinessProfiles", totalBusinessProfiles);
            metrics.put("totalDomains", totalDomains);

            // Fetch actual recent activity
            List<ActivityLog> recentLogs = activityLogRepository.findTop10ByOrderByTimestampDesc();
            List<Map<String, Object>> logData = recentLogs.stream().map(log -> {
                Map<String, Object> map = new HashMap<>();
                map.put("activity", log.getActivity());
                map.put("ipAddress", log.getIpAddress());
                map.put("timestamp", log.getTimestamp());
                
                String logEmail = "Unknown";
                if (log.getUser() != null) {
                    logEmail = log.getUser().getEmail() != null ? log.getUser().getEmail() : log.getUser().getUsername();
                    if (!logEmail.contains("@")) {
                        logEmail = logEmail + "@bnxmail.com";
                    }
                }
                map.put("email", logEmail);
                return map;
            }).collect(Collectors.toList());
            metrics.put("recentLogs", logData);

        } catch (Exception e) {
            log.error("Error fetching admin metrics: ", e);
        }
        return metrics;
    }

    public Page<Map<String, Object>> searchUsers(String query, Pageable pageable) {
        Page<User> users;
        if (query == null || query.trim().isEmpty()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.searchUsers(query, pageable);
        }
        
        return users.map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            
            // Ensure we display the full email handle
            String displayEmail = user.getEmail() != null ? user.getEmail() : user.getUsername();
            if (!displayEmail.contains("@")) {
                displayEmail = displayEmail + "@bnxmail.com";
            }
            
            map.put("email", displayEmail);
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("accountType", user.getAccountType());
            map.put("active", user.getActive());
            map.put("createdAt", user.getCreatedAt());
            return map;
        });
    }

    public Page<Map<String, Object>> getBannedUsers(Pageable pageable) {
        Page<User> users = userRepository.findByActiveFalse(pageable);
        
        return users.map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            
            String displayEmail = user.getEmail() != null ? user.getEmail() : user.getUsername();
            if (!displayEmail.contains("@")) {
                displayEmail = displayEmail + "@bnxmail.com";
            }
            
            map.put("email", displayEmail);
            map.put("firstName", user.getFirstName());
            map.put("lastName", user.getLastName());
            map.put("accountType", user.getAccountType());
            map.put("active", user.getActive());
            map.put("createdAt", user.getCreatedAt());

            List<com.btctech.mailapp.entity.Report> reports = reportRepository.findByReportedUserIdOrderByCreatedAtDesc(user.getId());
            List<Map<String, Object>> reportList = reports.stream().map(r -> {
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("reporterEmail", r.getReporter().getEmail() != null ? r.getReporter().getEmail() : r.getReporter().getUsername() + "@bnxmail.com");
                rMap.put("reason", r.getReason());
                rMap.put("emailSubject", r.getReportedEmailSubject());
                rMap.put("date", r.getCreatedAt());
                return rMap;
            }).collect(java.util.stream.Collectors.toList());
            map.put("reports", reportList);
            map.put("reportCount", reports.size());

            return map;
        });
    }

    public Page<Map<String, Object>> getAllReports(Pageable pageable) {
        Page<com.btctech.mailapp.entity.Report> reports = reportRepository.findAll(pageable);
        
        return reports.map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            
            // Reporter Details
            map.put("reporterId", r.getReporter().getId());
            map.put("reporterEmail", r.getReporter().getEmail() != null ? r.getReporter().getEmail() : r.getReporter().getUsername() + "@bnxmail.com");
            
            // Reported User Details
            map.put("reportedUserId", r.getReportedUser().getId());
            map.put("reportedUserEmail", r.getReportedUser().getEmail() != null ? r.getReportedUser().getEmail() : r.getReportedUser().getUsername() + "@bnxmail.com");
            map.put("reportedUserActive", r.getReportedUser().getActive());
            
            // Report Details
            map.put("reason", r.getReason());
            map.put("emailSubject", r.getReportedEmailSubject());
            map.put("date", r.getCreatedAt());
            
            return map;
        });
    }

    @Transactional(readOnly = true)
    public Page<Map<String, Object>> getAuditLogs(String query, Pageable pageable) {
        Page<ActivityLog> logs;
        if (query == null || query.trim().isEmpty()) {
            logs = activityLogRepository.findAllByOrderByTimestampDesc(pageable);
        } else {
            logs = activityLogRepository.searchLogs(query, pageable);
        }
        
        return logs.map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("activity", log.getActivity());
            map.put("details", log.getDetails());
            map.put("ipAddress", log.getIpAddress());
            map.put("deviceName", log.getDeviceName());
            map.put("timestamp", log.getTimestamp());
            
            if (log.getUser() != null) {
                String displayEmail = log.getUser().getEmail() != null ? log.getUser().getEmail() : log.getUser().getUsername();
                if (!displayEmail.contains("@")) {
                    displayEmail = displayEmail + "@bnxmail.com";
                }
                map.put("userEmail", displayEmail);
                map.put("userId", log.getUser().getId());
            } else {
                map.put("userEmail", "System");
                map.put("userId", null);
            }
            
            return map;
        });
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        user.setActive(!user.getActive());
        userRepository.save(user);
        
        // Log action
        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(user);
        logRecord.setActivity(user.getActive() ? "Account Unbanned by Admin" : "Account Suspended by Admin");
        activityLogRepository.save(logRecord);
    }

    @Transactional
    public void forceLogout(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        
        // Mark all refresh tokens as revoked to kill sessions
        refreshTokenRepository.findAllByUser(user).forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
        
        // Delete password sessions
        sessionService.deleteSessionsByUserId(user.getId());
        
        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(user);
        logRecord.setActivity("Forced Logout by Admin");
        logRecord.setDetails("Admin forced logout from User Management");
        activityLogRepository.save(logRecord);
    }

    @Transactional
    public void forceLogoutByEmail(String email, String adminUsername) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            user = userRepository.findByUsername(email).orElseThrow(() -> new RuntimeException("User not found"));
        }
        
        // Mark all refresh tokens as revoked to kill sessions
        refreshTokenRepository.findAllByUser(user).forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
        
        // Delete password sessions
        sessionService.deleteSessionsByUserId(user.getId());
        
        User admin = userRepository.findByUsername(adminUsername).orElse(null);
        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(admin != null ? admin : user);
        logRecord.setActivity("Targeted Force Logout");
        logRecord.setDetails("Admin forced logout of user: " + email);
        activityLogRepository.save(logRecord);
    }

    @Transactional
    public void resetUserPassword(Long userId, String newPassword, String adminUsername) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        User admin = userRepository.findByUsername(adminUsername).orElse(null);

        // Update password properly across all associated mail accounts
        userService.updateUserPassword(user, newPassword);

        // Invalidate all existing sessions so they have to log in with the new password
        refreshTokenRepository.deleteByUser(user);
        sessionService.deleteSessionsByUserId(user.getId());

        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(admin != null ? admin : user);
        logRecord.setActivity("Password Reset by Admin");
        logRecord.setDetails("Admin reset password for user: " + (user.getEmail() != null ? user.getEmail() : user.getUsername()));
        activityLogRepository.save(logRecord);
    }
}
