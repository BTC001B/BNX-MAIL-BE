package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.Organization;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BusinessService {

    private final OrganizationRepository organizationRepository;
    private final com.btctech.mailapp.repository.OrganizationInviteRepository inviteRepository;
    private final com.btctech.mailapp.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Initialize domain verification
     * Generates a token and returns DNS record instructions
     */
    @Transactional
    public Map<String, Object> initDomainVerification(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new MailException("Organization not found"));

        if (org.getVerified()) {
            throw new MailException("Domain already verified");
        }

        // Generate unique verification token
        String token = "bnx-verify-" + UUID.randomUUID().toString().substring(0, 8);
        org.setVerificationToken(token);
        organizationRepository.save(org);

        log.info("Initialized domain verification for: {} with token: {}", org.getDomain(), token);

        // Prepare DNS instructions
        Map<String, Object> result = new HashMap<>();
        result.put("organizationId", org.getId());
        result.put("domain", org.getDomain());
        result.put("verificationToken", token);
        
        Map<String, String> dnsRecords = new HashMap<>();
        dnsRecords.put("TXT", "bnx-verify=" + token);
        dnsRecords.put("MX", "mail.bnxmail.com");
        result.put("dnsRecords", dnsRecords);

        return result;
    }

    /**
     * Verify domain
     * For now, this is a simulation that sets verified = true
     */
    @Transactional
    public Map<String, Object> verifyDomain(Long organizationId) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new MailException("Organization not found"));

        if (org.getVerificationToken() == null) {
            throw new MailException("Verification not initialized. Call init first.");
        }

        // SIMULATION: In production, we would perform a DNS lookup here
        // e.g. using dnsjava or calling 'dig'
        log.info("Verifying domain: {}", org.getDomain());
        
        org.setVerified(true);
        organizationRepository.save(org);

        Map<String, Object> result = new HashMap<>();
        result.put("organizationId", org.getId());
        result.put("domain", org.getDomain());
        result.put("verified", true);
        result.put("message", "Domain verified successfully!");

        return result;
    }

    /**
     * Invite a new member to the organization
     */
    @Transactional
    public Map<String, Object> inviteMember(com.btctech.mailapp.entity.User admin, com.btctech.mailapp.dto.InviteMemberRequest request) {
        // 1. Authorization Check
        if (!"ORG_ADMIN".equals(admin.getRole())) {
            throw new MailException("Unauthorized: Only ORG_ADMIN can invite members");
        }

        Organization org = admin.getOrganization();
        if (org == null) {
            throw new MailException("Admin is not linked to any organization");
        }

        if (!org.getVerified()) {
            throw new MailException("Domain must be verified before inviting members");
        }

        String fullEmail = request.getEmailName() + "@" + org.getDomain();

        // 2. Generate Invite
        com.btctech.mailapp.entity.OrganizationInvite invite = new com.btctech.mailapp.entity.OrganizationInvite();
        invite.setOrganization(org);
        invite.setEmail(fullEmail);
        invite.setRole(request.getRole());
        invite.setInviteToken("org_invite_" + UUID.randomUUID().toString().substring(0, 12));
        invite.setExpiresAt(java.time.LocalDateTime.now().plusDays(7));
        
        invite = inviteRepository.save(invite);

        log.info("✓ Organization invite created for {} in organization {}", fullEmail, org.getName());

        Map<String, Object> result = new HashMap<>();
        result.put("inviteEmail", fullEmail);
        result.put("inviteToken", invite.getInviteToken());
        result.put("expiresAt", invite.getExpiresAt());
        
        return result;
    }

    /**
     * Accept an organization invitation
     */
    @Transactional
    public Map<String, Object> acceptInvite(com.btctech.mailapp.dto.AcceptInviteRequest request) {
        // 1. Validate Token
        com.btctech.mailapp.entity.OrganizationInvite invite = inviteRepository.findByInviteToken(request.getInviteToken())
                .orElseThrow(() -> new MailException("Invalid or expired invitation token"));

        if (invite.getAccepted()) {
            throw new MailException("Invitation has already been accepted");
        }

        if (invite.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new MailException("Invitation token has expired");
        }

        // 2. Create User
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new MailException("Username already exists");
        }

        com.btctech.mailapp.entity.User user = new com.btctech.mailapp.entity.User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(invite.getEmail()); // Use the email from invite
        user.setOrganization(invite.getOrganization());
        user.setAccountType(com.btctech.mailapp.entity.AccountType.BUSINESS);
        user.setRole(invite.getRole());
        user.setApproved(true);
        user.setActive(true);

        user = userRepository.save(user);

        // 3. Mark Invite as Accepted
        invite.setAccepted(true);
        inviteRepository.save(invite);

        log.info("✓ User {} joined organization {} via invite", user.getUsername(), invite.getOrganization().getName());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("email", user.getEmail());
        result.put("organizationId", invite.getOrganization().getId());
        result.put("message", "Welcome to the team! You can now create your email address.");

        return result;
    }

    /**
     * Get user by email (delegates to userRepository)
     */
    public com.btctech.mailapp.entity.User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("User not found: " + email));
    }
}
