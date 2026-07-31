package com.btctech.mailapp.service;

import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.repository.MailAccountRepository;
import com.btctech.mailapp.repository.RefreshTokenRepository;
import com.btctech.mailapp.repository.OrganizationRepository;
import com.btctech.mailapp.repository.BusinessProfileRepository;
import com.btctech.mailapp.repository.DomainRepository;
import com.btctech.mailapp.repository.ActivityLogRepository;
import com.btctech.mailapp.entity.ActivityLog;
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
        
        // Delete all refresh tokens to kill sessions
        refreshTokenRepository.deleteByUser(user);
        
        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(user);
        logRecord.setActivity("Forced Global Logout by Admin");
        activityLogRepository.save(logRecord);
    }
}
