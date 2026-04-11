package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.service.BusinessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/business")
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;

    /**
     * Domain Init API
     * Returns verification token and DNS instructions
     */
    @PostMapping("/domain/init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initDomain(
            @RequestBody Map<String, Long> request) {
        
        Long orgId = request.get("organizationId");
        log.info("Domain Init request for organizationId: {}", orgId);
        
        Map<String, Object> data = businessService.initDomainVerification(orgId);
        
        return ResponseEntity.ok(
                ApiResponse.success(data, "Domain verification initialized"));
    }

    /**
     * Domain Verify API
     * Confirms the verification status of the domain
     */
    @PostMapping("/domain/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyDomain(
            @RequestBody Map<String, Long> request) {
        
        Long orgId = request.get("organizationId");
        log.info("Domain Verify request for organizationId: {}", orgId);
        
        Map<String, Object> data = businessService.verifyDomain(orgId);
        
        return ResponseEntity.ok(
                ApiResponse.success(data, "Domain verified successfully"));
    }

    /**
     * Invite Member API
     * Only ORG_ADMIN can invite
     */
    @PostMapping("/invite-member")
    public ResponseEntity<ApiResponse<Map<String, Object>>> inviteMember(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody com.btctech.mailapp.dto.InviteMemberRequest request) {
        
        // In a real app, we'd get the user from the SecurityContext
        // For now, we'll look up based on the token principal (assume it's email)
        String token = authHeader.replace("Bearer ", "");
        String email = com.btctech.mailapp.config.JwtUtil.staticExtractUsername(token); 
        
        com.btctech.mailapp.entity.User admin = businessService.getUserByEmail(email);
        
        log.info("Invite Member request from admin: {}", admin.getUsername());
        
        Map<String, Object> data = businessService.inviteMember(admin, request);
        
        return ResponseEntity.ok(
                ApiResponse.success(data, "Invitation generated successfully"));
    }

    /**
     * Accept Invite API
     * Public endpoint to join an organization
     */
    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acceptInvite(
            @RequestBody com.btctech.mailapp.dto.AcceptInviteRequest request) {
        
        log.info("Accept Invite request for token: {}", request.getInviteToken());
        
        Map<String, Object> data = businessService.acceptInvite(request);
        
        return ResponseEntity.ok(
                ApiResponse.success(data, "Welcome to the team! Invitation accepted."));
    }
}
