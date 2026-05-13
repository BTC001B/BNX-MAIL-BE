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
    private final com.btctech.mailapp.repository.UserRepository userRepository;
    private final com.btctech.mailapp.repository.ExternalAppSessionRepository externalAppSessionRepository;
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

    @org.springframework.transaction.annotation.Transactional
    public String exchangeCodeForToken(String code, String clientId, String clientSecret, String ipAddress, String userAgent) {
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

        // 1. Get User
        com.btctech.mailapp.entity.User user = userRepository.findByEmail(data.email)
                .orElseGet(() -> userRepository.findByUsername(data.email)
                        .orElseThrow(() -> new RuntimeException("User not found")));

        // 2. Record SSO Session
        com.btctech.mailapp.entity.ExternalAppSession session = com.btctech.mailapp.entity.ExternalAppSession.builder()
                .user(user)
                .clientApp(client)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        externalAppSessionRepository.save(session);

        // 3. Remove code after single use
        codeStore.remove(code);

        // 4. Generate a new long-lived token for the client app
        return jwtUtil.generateToken(data.email);
    }

    private static record AuthCodeData(String clientId, String email, long expiry) {}
}
