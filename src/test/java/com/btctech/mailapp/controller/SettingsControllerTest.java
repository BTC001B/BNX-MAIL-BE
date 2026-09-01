package com.btctech.mailapp.controller;

import com.btctech.mailapp.config.JwtUtil;
import com.btctech.mailapp.dto.DefaultTextStyleRequest;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.entity.UserSettings;
import com.btctech.mailapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SettingsControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    private FakeJwtUtil jwtUtil;
    private FakeUserService userService;

    private User userA;
    private User userB;
    private UserSettings settingsA;
    private UserSettings settingsB;
    private String tokenA;
    private String tokenB;

    static class FakeJwtUtil extends JwtUtil {
        public FakeJwtUtil() {
            super();
        }

        @Override
        public String extractEmail(String token) {
            if ("valid.jwt.token.a".equals(token)) return "usera@bnxmail.com";
            if ("valid.jwt.token.b".equals(token)) return "userb@bnxmail.com";
            throw new RuntimeException("Invalid token");
        }
    }

    static class FakeUserService extends UserService {
        private final Map<String, User> users = new HashMap<>();
        private final Map<Long, UserSettings> userSettingsMap = new HashMap<>();
        public int updateSettingsCallCount = 0;
        public User lastUpdatedUser = null;

        public FakeUserService() {
            super(null, null, null, null, null, java.util.Collections.emptyList(), null);
        }

        public void addUser(User user, UserSettings settings) {
            users.put(user.getEmail(), user);
            users.put(user.getUsername(), user);
            userSettingsMap.put(user.getId(), settings);
        }

        @Override
        public User getUserByEmailOrUsername(String identifier) {
            User u = users.get(identifier);
            if (u == null) throw new RuntimeException("User not found: " + identifier);
            return u;
        }

        @Override
        public UserSettings getSettings(User user) {
            UserSettings s = userSettingsMap.get(user.getId());
            if (s == null) {
                s = UserSettings.builder()
                        .user(user)
                        .fontFamily("Arial")
                        .textStyleFontSize("Normal")
                        .textColor("#000000")
                        .build();
                userSettingsMap.put(user.getId(), s);
            }
            return s;
        }

        @Override
        public UserSettings updateSettings(User user, UserSettings newSettings) {
            updateSettingsCallCount++;
            lastUpdatedUser = user;
            UserSettings existing = getSettings(user);
            if (newSettings.getFontFamily() != null) existing.setFontFamily(newSettings.getFontFamily());
            if (newSettings.getTextStyleFontSize() != null) existing.setTextStyleFontSize(newSettings.getTextStyleFontSize());
            if (newSettings.getTextColor() != null) existing.setTextColor(newSettings.getTextColor());
            userSettingsMap.put(user.getId(), existing);
            return existing;
        }
    }

    @BeforeEach
    void setUp() {
        jwtUtil = new FakeJwtUtil();
        userService = new FakeUserService();

        tokenA = "Bearer valid.jwt.token.a";
        tokenB = "Bearer valid.jwt.token.b";

        userA = new User();
        userA.setId(1L);
        userA.setUsername("usera");
        userA.setEmail("usera@bnxmail.com");

        userB = new User();
        userB.setId(2L);
        userB.setUsername("userb");
        userB.setEmail("userb@bnxmail.com");

        settingsA = UserSettings.builder()
                .id(10L)
                .user(userA)
                .fontFamily("Arial")
                .textStyleFontSize("Normal")
                .textColor("#000000")
                .build();

        settingsB = UserSettings.builder()
                .id(11L)
                .user(userB)
                .fontFamily("Georgia")
                .textStyleFontSize("Large")
                .textColor("#0000FF")
                .build();

        userService.addUser(userA, settingsA);
        userService.addUser(userB, settingsB);

        SettingsController settingsController = new SettingsController(userService, jwtUtil);
        mockMvc = MockMvcBuilders.standaloneSetup(settingsController).build();
    }

    @Test
    @DisplayName("1. GET default text style for new authenticated user returns default values")
    void getDefaultTextStyle_newUser_returnsDefaults() throws Exception {
        mockMvc.perform(get("/api/settings/text-style")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Arial"))
                .andExpect(jsonPath("$.fontSize").value("Normal"))
                .andExpect(jsonPath("$.textColor").value("#000000"));
    }

    @Test
    @DisplayName("2. GET customized text style returns saved preferences")
    void getDefaultTextStyle_customized_returnsSavedValues() throws Exception {
        mockMvc.perform(get("/api/settings/text-style")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Georgia"))
                .andExpect(jsonPath("$.fontSize").value("Large"))
                .andExpect(jsonPath("$.textColor").value("#0000FF"));
    }

    @Test
    @DisplayName("3. PUT update font family successfully")
    void updateDefaultTextStyle_updateFontFamily_success() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Georgia")
                .fontSize("Normal")
                .textColor("#000000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Georgia"))
                .andExpect(jsonPath("$.fontSize").value("Normal"))
                .andExpect(jsonPath("$.textColor").value("#000000"))
                .andExpect(jsonPath("$.message").value("Default text style updated successfully"));
    }

    @Test
    @DisplayName("4. PUT update font size successfully")
    void updateDefaultTextStyle_updateFontSize_success() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Large")
                .textColor("#000000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontSize").value("Large"));
    }

    @Test
    @DisplayName("5. PUT update text color successfully")
    void updateDefaultTextStyle_updateTextColor_success() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Normal")
                .textColor("#FF0000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textColor").value("#FF0000"));
    }

    @Test
    @DisplayName("6. PUT update all three values together successfully")
    void updateDefaultTextStyle_updateAllThree_success() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Georgia")
                .fontSize("Large")
                .textColor("#0000FF")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Georgia"))
                .andExpect(jsonPath("$.fontSize").value("Large"))
                .andExpect(jsonPath("$.textColor").value("#0000FF"));
    }

    @Test
    @DisplayName("7. Invalid font family is rejected with 400 Bad Request")
    void updateDefaultTextStyle_invalidFontFamily_returnsBadRequest() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Comic Sans MS")
                .fontSize("Normal")
                .textColor("#000000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid font family"));
    }

    @Test
    @DisplayName("8. Invalid font size is rejected with 400 Bad Request")
    void updateDefaultTextStyle_invalidFontSize_returnsBadRequest() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Gigantic")
                .textColor("#000000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid font size"));
    }

    @Test
    @DisplayName("9. Invalid text color is rejected with 400 Bad Request")
    void updateDefaultTextStyle_invalidTextColor_returnsBadRequest() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Normal")
                .textColor("black123")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid text color"));
    }

    @Test
    @DisplayName("10. Unauthenticated GET request is rejected with 401 Unauthorized")
    void getDefaultTextStyle_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/settings/text-style"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("11. Unauthenticated PUT request is rejected with 401 Unauthorized")
    void updateDefaultTextStyle_unauthenticated_returnsUnauthorized() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Normal")
                .textColor("#000000")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("12 & 13. User A and User B settings isolation")
    void userIsolation_userA_and_userB_isolated() throws Exception {
        // User A GET
        mockMvc.perform(get("/api/settings/text-style")
                        .header("Authorization", tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Arial"))
                .andExpect(jsonPath("$.textColor").value("#000000"));

        // User B GET
        mockMvc.perform(get("/api/settings/text-style")
                        .header("Authorization", tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("Georgia"))
                .andExpect(jsonPath("$.textColor").value("#0000FF"));

        // User A update
        DefaultTextStyleRequest requestA = DefaultTextStyleRequest.builder()
                .fontFamily("Verdana")
                .fontSize("Small")
                .textColor("#333333")
                .build();

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().isOk());

        // Verify User A settings updated, User B settings unchanged
        org.junit.jupiter.api.Assertions.assertEquals("Verdana", userService.getSettings(userA).getFontFamily());
        org.junit.jupiter.api.Assertions.assertEquals("Georgia", userService.getSettings(userB).getFontFamily());
    }

    @Test
    @DisplayName("14. Idempotency: Repeated updates to user settings update existing record")
    void updateDefaultTextStyle_idempotent() throws Exception {
        DefaultTextStyleRequest request = DefaultTextStyleRequest.builder()
                .fontFamily("Arial")
                .fontSize("Normal")
                .textColor("#000000")
                .build();

        int initialCount = userService.updateSettingsCallCount;

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/settings/text-style")
                        .header("Authorization", tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        org.junit.jupiter.api.Assertions.assertEquals(initialCount + 2, userService.updateSettingsCallCount);
    }
}
