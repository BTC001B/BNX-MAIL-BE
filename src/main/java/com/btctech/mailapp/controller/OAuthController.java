package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.OAuthAuthorizeRequest;
import com.btctech.mailapp.dto.OAuthTokenRequest;
import com.btctech.mailapp.service.OAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/oauth")
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final JwtUtil jwtUtil;

    /**
     * Authenticated endpoint called by Auth UI after user logs in.
     * Returns an authorization code.
     */
    @PostMapping("/authorize")
    public ResponseEntity<ApiResponse<Map<String, String>>> authorize(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody OAuthAuthorizeRequest request) {

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);

        log.info("Generating auth code for client: {} and user: {}", request.getClientId(), email);

        String code = oAuthService.generateAuthorizationCode(
                request.getClientId(),
                request.getRedirectUri(),
                email
        );

        Map<String, String> data = new HashMap<>();
        data.put("code", code);
        data.put("state", request.getState());

        return ResponseEntity.ok(ApiResponse.success(data, "Authorization code generated"));
    }

    /**
     * Public endpoint called by Client App backend to exchange code for token.
     */
    @PostMapping("/token")
    public ResponseEntity<ApiResponse<Map<String, String>>> token(
            @Valid @RequestBody OAuthTokenRequest request) {

        log.info("Token exchange request for client: {}", request.getClientId());

        if (!"authorization_code".equals(request.getGrantType())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Unsupported grant type"));
        }

        String accessToken = oAuthService.exchangeCodeForToken(
                request.getCode(),
                request.getClientId(),
                request.getClientSecret()
        );

        Map<String, String> data = new HashMap<>();
        data.put("access_token", accessToken);
        data.put("token_type", "Bearer");

        return ResponseEntity.ok(ApiResponse.success(data, "Token generated successfully"));
    }
}
