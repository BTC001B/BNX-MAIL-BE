package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.MailLabel;
import com.btctech.mailapp.service.MailLabelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mail/labels")
@RequiredArgsConstructor
public class MailLabelController {

    private final MailLabelService labelService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MailLabel>>> getLabels(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(labelService.getLabels(email), "Labels retrieved"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MailLabel>> createLabel(
            @RequestBody MailLabel request,
            Authentication authentication) {
        String email = authentication.getName();
        MailLabel label = labelService.createLabel(email, request.getName(), request.getColorHex());
        return ResponseEntity.ok(ApiResponse.success(label, "Label created successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteLabel(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        labelService.deleteLabel(email, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Label deleted successfully"));
    }

    @PostMapping("/apply/{uid}")
    public ResponseEntity<ApiResponse<Void>> applyLabel(
            @PathVariable String uid,
            @RequestParam String folder,
            @RequestParam Long labelId,
            Authentication authentication) {
        String email = authentication.getName();
        labelService.applyLabelToEmail(email, uid, folder, labelId);
        return ResponseEntity.ok(ApiResponse.success(null, "Label applied successfully"));
    }

    @DeleteMapping("/remove/{uid}")
    public ResponseEntity<ApiResponse<Void>> removeLabel(
            @PathVariable String uid,
            @RequestParam String folder,
            @RequestParam Long labelId,
            Authentication authentication) {
        String email = authentication.getName();
        labelService.removeLabelFromEmail(email, uid, folder, labelId);
        return ResponseEntity.ok(ApiResponse.success(null, "Label removed successfully"));
    }
}
