package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.dto.SignatureDTO;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.service.SignatureService;
import com.btctech.mailapp.service.UserService;
import com.btctech.mailapp.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    private User getAuthenticatedUser(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String identifier = jwtUtil.extractEmail(token);
        return userService.getUserByEmailOrUsername(identifier);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SignatureDTO>>> getSignatures(
            @RequestHeader("Authorization") String authHeader) {
        User user = getAuthenticatedUser(authHeader);
        List<SignatureDTO> signatures = signatureService.getUserSignatures(user);
        return ResponseEntity.ok(ApiResponse.success(signatures, "Signatures retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SignatureDTO>> createSignature(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody SignatureDTO dto) {
        User user = getAuthenticatedUser(authHeader);
        SignatureDTO created = signatureService.createSignature(user, dto);
        return ResponseEntity.ok(ApiResponse.success(created, "Signature created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SignatureDTO>> updateSignature(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody SignatureDTO dto) {
        User user = getAuthenticatedUser(authHeader);
        SignatureDTO updated = signatureService.updateSignature(user, id, dto);
        return ResponseEntity.ok(ApiResponse.success(updated, "Signature updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSignature(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(authHeader);
        signatureService.deleteSignature(user, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Signature deleted successfully"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<SignatureDTO>> setDefaultSignature(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        User user = getAuthenticatedUser(authHeader);
        SignatureDTO updated = signatureService.setDefaultSignature(user, id);
        return ResponseEntity.ok(ApiResponse.success(updated, "Default signature updated successfully"));
    }
}
