package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.UserSettings;
import com.btctech.mailapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    private static final Set<String> SUPPORTED_LANGUAGES = new HashSet<>(Arrays.asList(
        "en", "ta", "hi", "te", "ml", "kn",
        "en_US", "ta_IN", "hi_IN", "te_IN", "ml_IN", "kn_IN"
    ));

    @GetMapping("/language")
    public ResponseEntity<?> getLanguagePreference(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }
        
        try {
            String token = authHeader.replace("Bearer ", "");
            String identifier = jwtUtil.extractEmail(token);
            User user = userService.getUserByEmailOrUsername(identifier);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            UserSettings settings = userService.getSettings(user);
            String lang = (settings != null && settings.getLanguage() != null) ? settings.getLanguage() : "en";
            
            // Normalize to short code if needed
            String normalizedLang = normalizeLanguageCode(lang);

            return ResponseEntity.ok(Map.of("language", normalizedLang));
        } catch (Exception e) {
            log.error("Error retrieving language preference: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    @PutMapping("/language")
    public ResponseEntity<?> updateLanguagePreference(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> payload) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        try {
            String token = authHeader.replace("Bearer ", "");
            String identifier = jwtUtil.extractEmail(token);
            User user = userService.getUserByEmailOrUsername(identifier);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }

            String language = payload != null ? payload.get("language") : null;

            if (language == null || !SUPPORTED_LANGUAGES.contains(language.trim())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid language code. Supported languages are: en, ta, hi, te, ml, kn"));
            }

            String normalizedLang = normalizeLanguageCode(language.trim());

            UserSettings update = UserSettings.builder()
                    .language(normalizedLang)
                    .build();

            userService.updateSettings(user, update);

            Map<String, String> response = new LinkedHashMap<>();
            response.put("message", "Language preference updated successfully");
            response.put("language", normalizedLang);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating language preference: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    private String normalizeLanguageCode(String lang) {
        if (lang == null) return "en";
        switch (lang.toLowerCase()) {
            case "ta":
            case "ta_in":
                return "ta";
            case "hi":
            case "hi_in":
                return "hi";
            case "te":
            case "te_in":
                return "te";
            case "ml":
            case "ml_in":
                return "ml";
            case "kn":
            case "kn_in":
                return "kn";
            case "en":
            case "en_us":
            default:
                return "en";
        }
    }
}
