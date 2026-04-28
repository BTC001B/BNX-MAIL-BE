package com.btctech.mailapp.service;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.entity.ClientApp;
import com.btctech.mailapp.repository.ClientAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OAuthService {

    private final ClientAppRepository clientAppRepository;
    private final JwtUtil jwtUtil;

    // In-memory store for authorization codes (code -> data)
    // In production, use Redis or Database with expiration
    private final Map<String, AuthCodeData> codeStore = new ConcurrentHashMap<>();

    public String generateAuthorizationCode(String clientId, String redirectUri, String email) {
        ClientApp client = clientAppRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Invalid client_id"));

        if (!client.getRedirectUri().equals(redirectUri)) {
            throw new RuntimeException("Invalid redirect_uri");
        }

        String code = UUID.randomUUID().toString();
        codeStore.put(code, new AuthCodeData(clientId, email, System.currentTimeMillis() + 300000)); // 5 mins expiry
        return code;
    }

    public String exchangeCodeForToken(String code, String clientId, String clientSecret) {
        AuthCodeData data = codeStore.get(code);
        if (data == null) {
            throw new RuntimeException("Invalid or expired authorization code");
        }

        if (System.currentTimeMillis() > data.expiry) {
            codeStore.remove(code);
            throw new RuntimeException("Authorization code expired");
        }

        ClientApp client = clientAppRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Invalid client_id"));

        if (!client.getClientSecret().equals(clientSecret)) {
            throw new RuntimeException("Invalid client_secret");
        }

        if (!data.clientId.equals(clientId)) {
            throw new RuntimeException("Code was not issued to this client");
        }

        // Remove code after single use
        codeStore.remove(code);

        // Generate a new long-lived token for the client app
        return jwtUtil.generateToken(data.email);
    }

    private static record AuthCodeData(String clientId, String email, long expiry) {}
}
