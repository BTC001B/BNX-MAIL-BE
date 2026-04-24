package com.btctech.mailapp.controller;

import com.btctech.mailapp.dto.ApiResponse;
import com.btctech.mailapp.entity.MailTemplate;
import com.btctech.mailapp.service.MailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/mail/templates")
@RequiredArgsConstructor
public class MailTemplateController {

    private final MailTemplateService templateService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MailTemplate>>> getTemplates(Authentication authentication) {
        String email = authentication.getName();
        List<MailTemplate> templates = templateService.getTemplates(email);
        return ResponseEntity.ok(ApiResponse.success(templates, "Templates retrieved successfully"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MailTemplate>> createTemplate(
            @RequestBody MailTemplate templateRequest,
            Authentication authentication) {
        String email = authentication.getName();
        MailTemplate template = templateService.createTemplate(
                email,
                templateRequest.getName(),
                templateRequest.getSubject(),
                templateRequest.getBody()
        );
        return ResponseEntity.ok(ApiResponse.success(template, "Template created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MailTemplate>> updateTemplate(
            @PathVariable Long id,
            @RequestBody MailTemplate templateRequest,
            Authentication authentication) {
        String email = authentication.getName();
        MailTemplate template = templateService.updateTemplate(
                id,
                email,
                templateRequest.getName(),
                templateRequest.getSubject(),
                templateRequest.getBody()
        );
        return ResponseEntity.ok(ApiResponse.success(template, "Template updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @PathVariable Long id,
            Authentication authentication) {
        String email = authentication.getName();
        templateService.deleteTemplate(id, email);
        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }
}
