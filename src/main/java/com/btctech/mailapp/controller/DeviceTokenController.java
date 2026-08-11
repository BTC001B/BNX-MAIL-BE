package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.DeviceTokenRequest;
import com.btctech.mailapp.entity.UserDevice;
import com.btctech.mailapp.repository.UserDeviceRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@Slf4j
public class DeviceTokenController {

    private final UserDeviceRepository userDeviceRepository;

    public DeviceTokenController(UserDeviceRepository userDeviceRepository) {
        this.userDeviceRepository = userDeviceRepository;
    }

    @PostMapping("/device-token")
    @Transactional
    public ResponseEntity<ApiResponse<String>> registerDeviceToken(
            @Valid @RequestBody DeviceTokenRequest request,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Registering device token for user: {}", userEmail);

            Optional<UserDevice> existingDevice = userDeviceRepository.findByDeviceToken(request.getDeviceToken());

            if (existingDevice.isPresent()) {
                UserDevice device = existingDevice.get();
                // If it belongs to the same user, just return success
                if (device.getUserEmail().equals(userEmail)) {
                    return ResponseEntity.ok(ApiResponse.success(null, "Device token already registered."));
                } else {
                    // Token belongs to another user (maybe they logged out and logged in as someone else)
                    // Update it to the current user
                    device.setUserEmail(userEmail);
                    device.setCreatedAt(LocalDateTime.now());
                    userDeviceRepository.save(device);
                    return ResponseEntity.ok(ApiResponse.success(null, "Device token updated to new user successfully."));
                }
            }

            // Create new device token entry
            UserDevice newDevice = UserDevice.builder()
                    .userEmail(userEmail)
                    .deviceToken(request.getDeviceToken())
                    .deviceType(request.getDeviceType().toLowerCase())
                    .createdAt(LocalDateTime.now())
                    .build();

            userDeviceRepository.save(newDevice);

            return ResponseEntity.ok(ApiResponse.success(null, "Device token registered successfully."));

        } catch (Exception e) {
            log.error("Failed to register device token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to register device token: " + e.getMessage()));
        }
    }

    @DeleteMapping("/device-token/{token}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> unregisterDeviceToken(
            @PathVariable String token,
            Authentication authentication) {
        
        try {
            String userEmail = authentication.getName();
            log.info("Unregistering device token for user: {}", userEmail);

            Optional<UserDevice> existingDevice = userDeviceRepository.findByDeviceToken(token);

            if (existingDevice.isPresent()) {
                if (existingDevice.get().getUserEmail().equals(userEmail)) {
                    userDeviceRepository.deleteByDeviceToken(token);
                    return ResponseEntity.ok(ApiResponse.success(null, "Device token unregistered successfully."));
                } else {
                    return ResponseEntity.status(403).body(ApiResponse.error("Token does not belong to the authenticated user."));
                }
            }

            return ResponseEntity.ok(ApiResponse.success(null, "Device token not found (already unregistered)."));

        } catch (Exception e) {
            log.error("Failed to unregister device token: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(ApiResponse.error("Failed to unregister device token: " + e.getMessage()));
        }
    }
}
