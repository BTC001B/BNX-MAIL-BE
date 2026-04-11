package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.UserService;
import com.btctech.mailapp.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * Parent Approval API
     * authenticated endpoint for parents to approve children
     */
    @PatchMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, Object>>> approveChild(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        
        String token = authHeader.replace("Bearer ", "");
        String identifier = JwtUtil.staticExtractUsername(token);
        
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
}
