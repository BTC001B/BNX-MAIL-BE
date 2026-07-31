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
}
