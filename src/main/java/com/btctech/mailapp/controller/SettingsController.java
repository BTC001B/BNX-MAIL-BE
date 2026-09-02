package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.DefaultTextStyleRequest;
import com.btctech.mailapp.dto.DefaultTextStyleResponse;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.UserSettings;
import com.btctech.mailapp.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;

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

    private static final Set<String> SUPPORTED_FONT_FAMILIES = new HashSet<>(Arrays.asList(
        "Arial", "Calibri", "Times New Roman", "Georgia", "Verdana", "Courier New", "Tahoma", "Trebuchet MS"
    ));

    private static final Set<String> SUPPORTED_FONT_SIZES = new HashSet<>(Arrays.asList(
        "Small", "Normal", "Large", "Extra Large"
    ));

    private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$");

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

    @GetMapping("/composing")
    public ResponseEntity<?> getComposingPreferences(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("spellingCheckEnabled", settings == null || settings.getSpellingCheckEnabled() == null || Boolean.TRUE.equals(settings.getSpellingCheckEnabled()));
            response.put("grammarCheckEnabled", settings == null || settings.getGrammarCheckEnabled() == null || Boolean.TRUE.equals(settings.getGrammarCheckEnabled()));
            response.put("autoCorrectEnabled", settings == null || settings.getAutoCorrectEnabled() == null || Boolean.TRUE.equals(settings.getAutoCorrectEnabled()));
            response.put("smartComposeEnabled", settings == null || settings.getSmartComposeEnabled() == null || Boolean.TRUE.equals(settings.getSmartComposeEnabled()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving composing preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    @PutMapping("/composing")
    public ResponseEntity<?> updateComposingPreferences(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> payload) {

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

            if (payload == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid request body"));
            }

            if (hasInvalidBooleanType(payload, "spellingCheckEnabled") ||
                hasInvalidBooleanType(payload, "grammarCheckEnabled") ||
                hasInvalidBooleanType(payload, "autoCorrectEnabled") ||
                hasInvalidBooleanType(payload, "smartComposeEnabled")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Composing settings fields must be boolean values"));
            }

            Boolean spellingCheckEnabled = parseBoolean(payload.get("spellingCheckEnabled"));
            Boolean grammarCheckEnabled = parseBoolean(payload.get("grammarCheckEnabled"));
            Boolean autoCorrectEnabled = parseBoolean(payload.get("autoCorrectEnabled"));
            Boolean smartComposeEnabled = parseBoolean(payload.get("smartComposeEnabled"));

            UserSettings update = UserSettings.builder()
                    .spellingCheckEnabled(spellingCheckEnabled)
                    .grammarCheckEnabled(grammarCheckEnabled)
                    .autoCorrectEnabled(autoCorrectEnabled)
                    .smartComposeEnabled(smartComposeEnabled)
                    .build();

            UserSettings saved = userService.updateSettings(user, update);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("message", "Composing preferences updated successfully");
            response.put("spellingCheckEnabled", Boolean.TRUE.equals(saved.getSpellingCheckEnabled()));
            response.put("grammarCheckEnabled", Boolean.TRUE.equals(saved.getGrammarCheckEnabled()));
            response.put("autoCorrectEnabled", Boolean.TRUE.equals(saved.getAutoCorrectEnabled()));
            response.put("smartComposeEnabled", Boolean.TRUE.equals(saved.getSmartComposeEnabled()));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating composing preferences: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    private Boolean parseBoolean(Object val) {
        if (val instanceof Boolean) return (Boolean) val;
        return null;
    }

    private boolean hasInvalidBooleanType(Map<String, Object> map, String key) {
        if (!map.containsKey(key)) return false;
        Object val = map.get(key);
        return val != null && !(val instanceof Boolean);
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

    @GetMapping("/text-style")
    public ResponseEntity<?> getDefaultTextStyle(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            String fontFamily = (settings != null && settings.getFontFamily() != null) ? settings.getFontFamily() : "Arial";
            String fontSize = (settings != null && settings.getTextStyleFontSize() != null) ? settings.getTextStyleFontSize() : "Normal";
            String textColor = (settings != null && settings.getTextColor() != null) ? settings.getTextColor() : "#000000";

            DefaultTextStyleResponse response = DefaultTextStyleResponse.builder()
                    .fontFamily(fontFamily)
                    .fontSize(fontSize)
                    .textColor(textColor)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving default text style: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    @PutMapping("/text-style")
    public ResponseEntity<?> updateDefaultTextStyle(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) DefaultTextStyleRequest payload) {

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

            if (payload == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Request body is required"));
            }

            String fontFamily = payload.getFontFamily();
            String fontSize = payload.getFontSize();
            String textColor = payload.getTextColor();

            if (fontFamily == null || !SUPPORTED_FONT_FAMILIES.contains(fontFamily.trim())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid font family"));
            }

            if (fontSize == null || !SUPPORTED_FONT_SIZES.contains(fontSize.trim())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid font size"));
            }

            if (textColor == null || !HEX_COLOR_PATTERN.matcher(textColor.trim()).matches()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "Invalid text color"));
            }

            UserSettings update = UserSettings.builder()
                    .fontFamily(fontFamily.trim())
                    .textStyleFontSize(fontSize.trim())
                    .textColor(textColor.trim())
                    .build();

            UserSettings saved = userService.updateSettings(user, update);

            DefaultTextStyleResponse response = DefaultTextStyleResponse.builder()
                    .fontFamily(saved.getFontFamily())
                    .fontSize(saved.getTextStyleFontSize())
                    .textColor(saved.getTextColor())
                    .message("Default text style updated successfully")
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating default text style: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    @GetMapping("/wallpaper")
    public ResponseEntity<?> getWallpaperPreference(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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
            String wallpaper = (settings != null && settings.getWallpaper() != null) ? settings.getWallpaper() : "default";

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("wallpaper", wallpaper);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error retrieving wallpaper preference: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid or expired token"));
        }
    }

    @PutMapping("/wallpaper")
    public ResponseEntity<?> updateWallpaperPreference(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody(required = false) Map<String, String> payload) {

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

            String wallpaper = (payload != null && payload.get("wallpaper") != null) ? payload.get("wallpaper").trim() : "default";

            UserSettings update = UserSettings.builder()
                    .wallpaper(wallpaper)
                    .build();

            UserSettings saved = userService.updateSettings(user, update);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Wallpaper updated successfully");
            response.put("wallpaper", saved.getWallpaper() != null ? saved.getWallpaper() : "default");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating wallpaper preference: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to update wallpaper preference: " + e.getMessage()));
        }
    }

    @RequestMapping(value = "/wallpaper/reset", method = {RequestMethod.POST, RequestMethod.PUT})
    public ResponseEntity<?> resetWallpaperToDefault(@RequestHeader(value = "Authorization", required = false) String authHeader) {
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

            UserSettings saved = userService.resetWallpaper(user);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Wallpaper reset to default");
            response.put("wallpaper", saved.getWallpaper() != null ? saved.getWallpaper() : "default");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting wallpaper preference: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "error", "Failed to reset wallpaper: " + e.getMessage()));
        }
    }
}
