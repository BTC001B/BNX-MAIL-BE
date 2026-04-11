package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.AccountType;
import com.btctech.mailapp.entity.Organization;
import com.btctech.mailapp.dto.RegisterRequest;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationService organizationService;

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
     * Create user
     */
    @Transactional
    public User createUser(RegisterRequest request) {
        // Validate
        validateUsername(request.getUsername());

        AccountType type = AccountType.valueOf(request.getAccountType().toUpperCase());

        switch (type) {
            case PUBLIC:
                return createPublicUser(request);
            case BUSINESS:
                return createBusinessUser(request);
            case CHILD:
                return createChildUser(request);
            default:
                throw new MailException("Unsupported account type: " + type);
        }
    }

    private User createPublicUser(RegisterRequest request) {
        User user = createBaseUser(request);
        user.setAccountType(AccountType.PUBLIC);
        user.setRole("PUBLIC_USER");
        user.setApproved(true);
        user = userRepository.save(user);
        log.info("Created PUBLIC user: {}", user.getUsername());
        return user;
    }

    private User createBusinessUser(RegisterRequest request) {
        if (request.getCompanyName() == null || request.getDomain() == null) {
            throw new MailException("Company name and domain are required for Business accounts");
        }

        // 1. Create Organization
        Organization org = organizationService.createOrganization(
                request.getCompanyName(),
                request.getDomain()
        );

        // 2. Create User linked to Org
        User user = createBaseUser(request);
        user.setAccountType(AccountType.BUSINESS);
        user.setOrganization(org);
        user.setRole("ORG_ADMIN"); // Business creator is admin
        user.setApproved(true);

        user = userRepository.save(user);
        log.info("Created BUSINESS user: {} (ORG_ADMIN) for organization: {}", user.getUsername(), org.getName());
        return user;
    }

    private User createChildUser(RegisterRequest request) {
        if (request.getParentEmail() == null || request.getDob() == null) {
            throw new MailException("Parent email and Date of Birth are required for Child accounts");
        }

        // 1. Find parent
        User parent = userRepository.findByEmail(request.getParentEmail())
                .orElseThrow(() -> new MailException("Parent account not found: " + request.getParentEmail()));

        // 2. Create Child User
        User user = createBaseUser(request);
        user.setAccountType(AccountType.CHILD);
        user.setParent(parent);
        user.setDob(request.getDob());
        user.setRole("CHILD_USER");
        user.setApproved(false); // Child needs parental approval

        // 3. Update Parent's role to PARENT if not already ADMIN/PARENT
        if (!"ORG_ADMIN".equals(parent.getRole()) && !"PARENT".equals(parent.getRole())) {
            parent.setRole("PARENT");
            userRepository.save(parent);
        }

        user = userRepository.save(user);
        log.info("Created CHILD user: {} linked to parent: {}", user.getUsername(), parent.getUsername());
        return user;
    }

    private User createBaseUser(RegisterRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setActive(true);
        return user;
    }

    /**
     * Authenticate user by email
     */
    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("Invalid credentials"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new MailException("Invalid credentials");
        }

        if (!user.getActive()) {
            throw new MailException("Account is disabled");
        }

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User authenticated: {}", email);
        return user;
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
            return getUserByEmail(identifier);
        } else {
            return getUserByUsername(identifier);
        }
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