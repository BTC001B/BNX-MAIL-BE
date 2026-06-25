package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.EmailTemplate;
import com.btctech.mailapp.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;

    private String resolveUserEmail(String paramEmail, Authentication authentication) {
        if (paramEmail != null && !paramEmail.trim().isEmpty()) {
            return paramEmail.trim();
        }
        if (authentication != null && authentication.getName() != null) {
            return authentication.getName();
        }
        throw new RuntimeException("User email is required");
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<EmailTemplate>>> getTemplates(
            @RequestParam(required = false) String userEmail,
            Authentication authentication) {
        String email = resolveUserEmail(userEmail, authentication);
        List<EmailTemplate> templates = emailTemplateService.getTemplatesByUser(email);
        return ResponseEntity.ok(ApiResponse.success(templates, "Templates retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<EmailTemplate>> createTemplate(
            @RequestParam(required = false) String userEmail,
            @RequestBody EmailTemplate template,
            Authentication authentication) {
        String email = resolveUserEmail(userEmail, authentication);
        EmailTemplate created = emailTemplateService.createTemplate(email, template);
        return ResponseEntity.ok(ApiResponse.success(created, "Template created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EmailTemplate>> updateTemplate(
            @RequestParam(required = false) String userEmail,
            @PathVariable Long id,
            @RequestBody EmailTemplate template,
            Authentication authentication) {
        String email = resolveUserEmail(userEmail, authentication);
        EmailTemplate updated = emailTemplateService.updateTemplate(email, id, template);
        return ResponseEntity.ok(ApiResponse.success(updated, "Template updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @RequestParam(required = false) String userEmail,
            @PathVariable Long id,
            Authentication authentication) {
        String email = resolveUserEmail(userEmail, authentication);
        emailTemplateService.deleteTemplate(email, id);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }
}
