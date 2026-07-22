package com.btctech.mailapp.controller;

import com.btctech.mailapp.entity.AuthenticatorAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.repository.AuthenticatorAccountRepository;
import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.service.TwoFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Transactional
@RestController
@RequestMapping("/api/users/2fa")
public class TwoFactorController {

    @Autowired
    private TwoFactorService twoFactorService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthenticatorAccountRepository authenticatorAccountRepository;

    @PostMapping("/setup")
    public ResponseEntity<?> initiateSetup(@AuthenticationPrincipal String principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        String secret = twoFactorService.generateSecret();
        String label = (user.getEmail() != null && !user.getEmail().trim().isEmpty()) ? user.getEmail() : user.getUsername();
        String qrCodeUrl = twoFactorService.getQrCodeUrl(secret, label);

        // We don't save yet, just return to frontend for verification
        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeUrl", qrCodeUrl);

        // Store secret temporarily in the user entity (or a cache)
        user.setTwoFactorSecret(secret);
        userRepository.save(user);

        Map<String, Object> finalResponse = new HashMap<>();
        finalResponse.put("success", true);
        finalResponse.put("data", response);
        
        return ResponseEntity.ok(finalResponse);
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyAndEnable(@AuthenticationPrincipal String principal, @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (twoFactorService.verifyCode(user.getTwoFactorSecret(), code)) {
            user.setTwoFactorEnabled(true);
            userRepository.save(user);

            // Also add this to their synced authenticator accounts
            AuthenticatorAccount acc = new AuthenticatorAccount();
            acc.setUser(user);
            acc.setAccountName("BNX Auth (" + user.getEmail() + ")");
            acc.setSecretKey(user.getTwoFactorSecret());
            authenticatorAccountRepository.save(acc);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "2FA enabled successfully");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid code");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> getAuthenticatorAccounts(@AuthenticationPrincipal String principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<AuthenticatorAccount> accounts = new java.util.ArrayList<>(authenticatorAccountRepository.findByUser(user));
        
        // Filter out BNX Auth account if 2FA is disabled for current user
        if (!Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            String bnxAuthName = "BNX Auth (" + user.getEmail() + ")";
            accounts.removeIf(acc -> bnxAuthName.equals(acc.getAccountName()));
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> addAuthenticatorAccount(@AuthenticationPrincipal String principal, @RequestBody Map<String, String> body) {
        String name = body.get("name");
        String secret = body.get("secret");
        
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthenticatorAccount acc = new AuthenticatorAccount();
        acc.setUser(user);
        acc.setAccountName(name);
        acc.setSecretKey(secret);
        authenticatorAccountRepository.save(acc);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/disable")
    public ResponseEntity<?> disable2FA(@AuthenticationPrincipal String principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setTwoFactorEnabled(false);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "2FA disabled successfully");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/enable")
    public ResponseEntity<?> enable2FA(@AuthenticationPrincipal String principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getTwoFactorSecret() == null) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "No 2FA setup found"));
        }

        user.setTwoFactorEnabled(true);
        userRepository.save(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "2FA enabled successfully");
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/accounts/{id}")
    public ResponseEntity<?> deleteAuthenticatorAccount(@AuthenticationPrincipal String principal, @PathVariable Long id) {
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("success", false, "message", "Unauthorized"));
        }
        User user = userRepository.findByUsername(principal)
                .or(() -> userRepository.findByEmail(principal))
                .orElseThrow(() -> new RuntimeException("User not found"));

        AuthenticatorAccount acc = authenticatorAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!acc.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Forbidden"));
        }

        authenticatorAccountRepository.delete(acc);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
}
