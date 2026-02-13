package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.*;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
import com.btctech.mailapp.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final MailboxService mailboxService;
    private final JwtUtil jwtUtil;
    private final SessionService sessionService;

    /**
     * STEP 1: Register user (username + password)
     * Returns temporary token for email creation
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("Registration request for username: {}", request.getUsername());

        // Create user
        User user = userService.createUser(request);

        // Generate temporary token (valid for email creation)
        String tempToken = jwtUtil.generateToken("temp_" + user.getUsername());

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("tempToken", tempToken);
        data.put("message", "Registration successful! Now create your email address.");

        return ResponseEntity.ok(
                ApiResponse.success(data, "User registered successfully"));
    }

    /**
     * STEP 3: Login with EMAIL + password
     */
    @PostMapping("/login")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request for: {}", request.getEmail());

        // Authenticate
        User user = userService.authenticate(request.getEmail(), request.getPassword());

        // Get mail account
        MailAccount mailAccount = mailboxService.getMailAccountByEmail(request.getEmail());

        // Generate JWT
        String token = jwtUtil.generateToken(request.getEmail());

        // Create session (store password encrypted)
        sessionService.createSession(user.getId(), mailAccount.getId(),
                request.getPassword(), token);

        // Prepare response
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("email", request.getEmail());
        data.put("username", user.getUsername());
        data.put("firstName", user.getFirstName());
        data.put("lastName", user.getLastName());

        return ResponseEntity.ok(
                ApiResponse.success(data, "Login successful"));
    }
}
