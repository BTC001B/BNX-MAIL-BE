package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.UserService;
import com.btctech.mailapp.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * Parent Approval API
     * authenticated endpoint for parents to approve children
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveChild(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = authHeader.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(token);
        
        // Identify caller (Parent)
        User parent = userService.getUserByEmailOrUsername(identifier);
        
        log.info("Approval request for child userId: {} from parent: {}", id, parent.getUsername());
        
        User approvedChild = userService.approveChild(parent, id);
        
        Map<String, Object> data = new HashMap<>();
        data.put("userId", approvedChild.getId());
        data.put("username", approvedChild.getUsername());
        data.put("approved", approvedChild.getApproved());
        data.put("approvedAt", approvedChild.getApprovedAt());
        
        return ResponseEntity.ok(ApiResponse.success(data, "Child account approved successfully"));
    }

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<com.btctech.mailapp.dto.UserSettingsDTO>> getSettings(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmailOrUsername(identifier);
        
        com.btctech.mailapp.entity.UserSettings settings = userService.getSettings(user);
        
        com.btctech.mailapp.dto.UserSettingsDTO dto = com.btctech.mailapp.dto.UserSettingsDTO.builder()
                .phoneNumber(settings.getPhoneNumber())
                .location(settings.getLocation())
                .jobTitle(settings.getJobTitle())
                .inboxNotifications(settings.getInboxNotifications())
                .sentNotifications(settings.getSentNotifications())
                .starredNotifications(settings.getStarredNotifications())
                .snoozedNotifications(settings.getSnoozedNotifications())
                .soundEnabled(settings.getSoundEnabled())
                .vibrationEnabled(settings.getVibrationEnabled())
                .quietHoursEnabled(settings.getQuietHoursEnabled())
                .quietHoursStart(settings.getQuietHoursStart())
                .quietHoursEnd(settings.getQuietHoursEnd())
                .themeMode(settings.getThemeMode())
                .accentColor(settings.getAccentColor())
                .fontSize(settings.getFontSize())
                .density(settings.getDensity())
                .storageLimit(settings.getStorageLimit())
                .twoFactorEnabled(settings.getTwoFactorEnabled())
                .biometricsEnabled(settings.getBiometricsEnabled())
                .language(settings.getLanguage())
                .build();
                
        return ResponseEntity.ok(ApiResponse.success(dto, "Settings retrieved successfully"));
    }

    @PatchMapping("/settings")
    public ResponseEntity<ApiResponse<com.btctech.mailapp.dto.UserSettingsDTO>> updateSettings(
            HttpServletRequest request,
            @RequestHeader("Authorization") String authHeader,
            @RequestBody com.btctech.mailapp.dto.UserSettingsDTO settingsUpdate) {
        
        String token = authHeader.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmailOrUsername(identifier);
        
        com.btctech.mailapp.entity.UserSettings update = com.btctech.mailapp.entity.UserSettings.builder()
                .phoneNumber(settingsUpdate.getPhoneNumber())
                .location(settingsUpdate.getLocation())
                .jobTitle(settingsUpdate.getJobTitle())
                .inboxNotifications(settingsUpdate.getInboxNotifications())
                .sentNotifications(settingsUpdate.getSentNotifications())
                .starredNotifications(settingsUpdate.getStarredNotifications())
                .snoozedNotifications(settingsUpdate.getSnoozedNotifications())
                .soundEnabled(settingsUpdate.getSoundEnabled())
                .vibrationEnabled(settingsUpdate.getVibrationEnabled())
                .quietHoursEnabled(settingsUpdate.getQuietHoursEnabled())
                .quietHoursStart(settingsUpdate.getQuietHoursStart())
                .quietHoursEnd(settingsUpdate.getQuietHoursEnd())
                .themeMode(settingsUpdate.getThemeMode())
                .accentColor(settingsUpdate.getAccentColor())
                .fontSize(settingsUpdate.getFontSize())
                .density(settingsUpdate.getDensity())
                .twoFactorEnabled(settingsUpdate.getTwoFactorEnabled())
                .biometricsEnabled(settingsUpdate.getBiometricsEnabled())
                .language(settingsUpdate.getLanguage())
                .build();
                
        com.btctech.mailapp.entity.UserSettings saved = userService.updateSettings(user, update);
        
        // Log activity
        userService.logActivity(user, "Settings Updated", "Updated security/appearance settings", 
            request.getHeader("X-Forwarded-For"), request.getHeader("X-Device-Name"));
        
        // Re-build DTO
        com.btctech.mailapp.dto.UserSettingsDTO responseDto = com.btctech.mailapp.dto.UserSettingsDTO.builder()
                .phoneNumber(saved.getPhoneNumber())
                .location(saved.getLocation())
                .jobTitle(saved.getJobTitle())
                .inboxNotifications(saved.getInboxNotifications())
                .sentNotifications(saved.getSentNotifications())
                .starredNotifications(saved.getStarredNotifications())
                .snoozedNotifications(saved.getSnoozedNotifications())
                .soundEnabled(saved.getSoundEnabled())
                .vibrationEnabled(saved.getVibrationEnabled())
                .quietHoursEnabled(saved.getQuietHoursEnabled())
                .quietHoursStart(saved.getQuietHoursStart())
                .quietHoursEnd(saved.getQuietHoursEnd())
                .themeMode(saved.getThemeMode())
                .accentColor(saved.getAccentColor())
                .fontSize(saved.getFontSize())
                .density(saved.getDensity())
                .storageLimit(saved.getStorageLimit())
                .twoFactorEnabled(saved.getTwoFactorEnabled())
                .biometricsEnabled(saved.getBiometricsEnabled())
                .language(saved.getLanguage())
                .build();
                
        return ResponseEntity.ok(ApiResponse.success(responseDto, "Settings updated successfully"));
    }

    @GetMapping("/activity-logs")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getActivityLogs(
            @RequestHeader("Authorization") String authHeader) {
        
        String token = authHeader.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(token);
        User user = userService.getUserByEmailOrUsername(identifier);
        
        List<com.btctech.mailapp.entity.ActivityLog> logs = userService.getActivityLogs(user);
        
        List<Map<String, Object>> result = logs.stream().map(l -> {
            Map<String, Object> map = new HashMap<>();
            map.put("activity", l.getActivity());
            map.put("details", l.getDetails());
            map.put("timestamp", l.getTimestamp().toString());
            map.put("ipAddress", l.getIpAddress());
            map.put("deviceName", l.getDeviceName());
            return map;
        }).collect(java.util.stream.Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(result, "Activity logs retrieved successfully"));
    }
}
