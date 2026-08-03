package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import com.btctech.mailapp.service.AdminService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
// @PreAuthorize("hasRole('ADMIN')") // Uncomment when roles are implemented on User entity
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardMetrics() {
        Map<String, Object> metrics = adminService.getDashboardMetrics();
        return ResponseEntity.ok(ApiResponse.success(metrics, "Metrics fetched successfully"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> searchUsers(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        Page<Map<String, Object>> users = adminService.searchUsers(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(users, "Users fetched successfully"));
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getAuditLogs(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        Page<Map<String, Object>> logs = adminService.getAuditLogs(query, pageable);
        return ResponseEntity.ok(ApiResponse.success(logs, "Audit logs fetched successfully"));
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<String>> toggleUserStatus(@PathVariable Long id) {
        adminService.toggleUserStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Status updated", "User status toggled successfully"));
    }

    @PostMapping("/users/{id}/logout")
    public ResponseEntity<ApiResponse<String>> forceLogoutUser(@PathVariable Long id) {
        adminService.forceLogout(id);
        return ResponseEntity.ok(ApiResponse.success("Logout forced", "User sessions destroyed successfully"));
    }

    @GetMapping("/cases/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAbuseCase(@PathVariable Long id) {
        Map<String, Object> caseData = adminService.getAbuseCase(id);
        return ResponseEntity.ok(ApiResponse.success(caseData, "Case fetched successfully"));
    }

    @PutMapping("/cases/{id}/decide")
    public ResponseEntity<ApiResponse<String>> decideAbuseCase(
            @PathVariable Long id, 
            @RequestBody Map<String, String> payload) {
        String decision = payload.get("decision");
        if (decision == null || (!decision.equals("UNBAN") && !decision.equals("BAN"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid decision. Must be UNBAN or BAN."));
        }
        
        adminService.decideAbuseCase(id, decision);
        return ResponseEntity.ok(ApiResponse.success("Decision recorded", "Case successfully concluded."));
    }

    @PostMapping("/system/broadcast")
    public ResponseEntity<ApiResponse<String>> sendGlobalBroadcast(@RequestBody Map<String, String> payload) {
        String adminUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String subject = payload.get("subject");
        String message = payload.get("message");
        
        if (subject == null || message == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Subject and message are required"));
        }
        
        adminService.sendGlobalBroadcast(adminUsername, subject, message);
        return ResponseEntity.ok(ApiResponse.success("Broadcast initiated", "The email is being sent to all active users."));
    }

    @PostMapping("/system/force-logout-all")
    public ResponseEntity<ApiResponse<String>> forceGlobalLogoutAll() {
        String adminUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.forceGlobalLogoutAll(adminUsername);
        return ResponseEntity.ok(ApiResponse.success("Global Logout Executed", "All sessions across the platform have been destroyed."));
    }

    @PostMapping("/system/force-logout-by-email")
    public ResponseEntity<ApiResponse<String>> forceLogoutByEmail(@RequestBody Map<String, String> payload) {
        String adminUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String email = payload.get("email");
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Email/username is required"));
        }
        try {
            adminService.forceLogoutByEmail(email, adminUsername);
            return ResponseEntity.ok(ApiResponse.success("Logout Executed", "User session destroyed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to force logout: " + e.getMessage()));
        }
    }

    @GetMapping("/system/settings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSystemSettings() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSystemSettings(), "Settings fetched successfully"));
    }

    @PutMapping("/system/settings")
    public ResponseEntity<ApiResponse<String>> updateSystemSettings(@RequestBody Map<String, String> payload) {
        String adminUsername = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        adminService.updateSystemSettings(adminUsername, payload);
        return ResponseEntity.ok(ApiResponse.success("Settings Updated", "System settings have been successfully updated."));
    }
}
