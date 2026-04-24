package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.MailTemplate;
import com.btctech.mailapp.repository.MailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailTemplateService {

    private final MailTemplateRepository templateRepository;

    public List<MailTemplate> getTemplates(String userEmail) {
        log.info("Fetching templates for user: {}", userEmail);
        return templateRepository.findByUserEmailOrderByNameAsc(userEmail);
    }

    public MailTemplate createTemplate(String userEmail, String name, String subject, String body) {
        log.info("Creating new template '{}' for user: {}", name, userEmail);
        MailTemplate template = MailTemplate.builder()
                .userEmail(userEmail)
                .name(name)
                .subject(subject)
                .body(body)
                .build();
        return templateRepository.save(template);
    }

    public MailTemplate updateTemplate(Long id, String userEmail, String name, String subject, String body) {
        log.info("Updating template ID: {} for user: {}", id, userEmail);
        Optional<MailTemplate> existing = templateRepository.findById(id);
        
        if (existing.isPresent() && existing.get().getUserEmail().equals(userEmail)) {
            MailTemplate template = existing.get();
            template.setName(name);
            template.setSubject(subject);
            template.setBody(body);
            return templateRepository.save(template);
        }
        throw new RuntimeException("Template not found or unauthorized");
    }

    public void deleteTemplate(Long id, String userEmail) {
        log.info("Deleting template ID: {} for user: {}", id, userEmail);
        Optional<MailTemplate> existing = templateRepository.findById(id);
        
        if (existing.isPresent() && existing.get().getUserEmail().equals(userEmail)) {
            templateRepository.delete(existing.get());
        } else {
            throw new RuntimeException("Template not found or unauthorized");
        }
    }
}
