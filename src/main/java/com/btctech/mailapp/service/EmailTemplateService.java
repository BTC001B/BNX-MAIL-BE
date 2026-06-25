package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.EmailTemplate;
import com.btctech.mailapp.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository emailTemplateRepository;

    public List<EmailTemplate> getTemplatesByUser(String userEmail) {
        log.info("Fetching custom email templates for user: {}", userEmail);
        return emailTemplateRepository.findByUserEmail(userEmail);
    }

    @Transactional
    public EmailTemplate createTemplate(String userEmail, EmailTemplate request) {
        log.info("Creating custom email template for user: {}", userEmail);
        EmailTemplate template = EmailTemplate.builder()
                .userEmail(userEmail)
                .title(request.getTitle())
                .subject(request.getSubject())
                .body(request.getBody())
                .category(request.getCategory() != null ? request.getCategory() : "Custom")
                .isDefault(false)
                .build();
        return emailTemplateRepository.save(template);
    }

    @Transactional
    public EmailTemplate updateTemplate(String userEmail, Long id, EmailTemplate request) {
        log.info("Updating custom email template ID: {} for user: {}", id, userEmail);
        EmailTemplate template = emailTemplateRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Email template not found or unauthorized"));
        
        template.setTitle(request.getTitle());
        template.setSubject(request.getSubject());
        template.setBody(request.getBody());
        if (request.getCategory() != null) {
            template.setCategory(request.getCategory());
        }
        return emailTemplateRepository.save(template);
    }

    @Transactional
    public void deleteTemplate(String userEmail, Long id) {
        log.info("Deleting custom email template ID: {} for user: {}", id, userEmail);
        EmailTemplate template = emailTemplateRepository.findByIdAndUserEmail(id, userEmail)
                .orElseThrow(() -> new RuntimeException("Email template not found or unauthorized"));
        emailTemplateRepository.delete(template);
    }
}
