package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.service.BusinessService;
import com.btctech.mailapp.config.JwtUtil;
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
    private final JwtUtil jwtUtil;
    private final com.btctech.mailapp.repository.BusinessProfileRepository businessProfileRepository;
    private final com.btctech.mailapp.repository.UserRepository userRepository;

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
        
        // For now, we'll look up based on the token principal (assume it's email)
        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token); 
        
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

    /**
     * Business Onboarding API
     */
    @PostMapping("/onboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> onboardBusiness(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody com.btctech.mailapp.dto.BusinessOnboardingRequest request) {

        String token = authHeader.replace("Bearer ", "");
        String email = jwtUtil.extractEmail(token); 
        com.btctech.mailapp.entity.User user = businessService.getUserByEmail(email);

        log.info("Onboarding request for business user: {}", user.getUsername());

        com.btctech.mailapp.entity.BusinessProfile profile = businessProfileRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    com.btctech.mailapp.entity.BusinessProfile bp = new com.btctech.mailapp.entity.BusinessProfile();
                    bp.setUser(user);
                    bp.setBusinessName(user.getOrganization() != null ? user.getOrganization().getName() : "My Business");
                    return bp;
                });

        profile.setBusinessType(request.getBusinessType());
        profile.setIndustry(request.getIndustry());
        profile.setCompanySize(request.getCompanySize());
        profile.setBusinessWebsite(request.getBusinessWebsite());
        profile.setBusinessAddress(request.getBusinessAddress());
        profile.setTimeZone(request.getTimeZone());
        profile.setLanguage(request.getLanguage());
        profile.setCompanyLogo(request.getCompanyLogo());
        profile.setProfilePhoto(request.getProfilePhoto());
        profile.setAcceptTerms(request.getAcceptTerms());
        profile.setOnboarded(true);

        businessProfileRepository.save(profile);

        // Also update the general profile_picture of user if profile photo is uploaded
        if (request.getProfilePhoto() != null && !request.getProfilePhoto().isEmpty()) {
            user.setProfilePicture(request.getProfilePhoto());
            userRepository.save(user);
        }

        Map<String, Object> data = Map.of(
            "onboarded", true,
            "businessName", profile.getBusinessName(),
            "businessType", profile.getBusinessType()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "Business profile onboarded successfully"));
    }
}
