package com.btctech.mailapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Gatekeeper for file security and compliance.
 * Orchestrates Virus Scanning and future Quota Enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentSecurityService {

    private final ClamAVService clamAVService;
    private final com.btctech.mailapp.repository.MailAccountRepository mailAccountRepository;

    /**
     * Validate file for security threats (Virus Scan)
     */
    public void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            return;
        }

        log.info("Security validation for file: {} (Size: {} bytes)", file.getOriginalFilename(), file.getSize());
        
        try {
            // Virus Scan: Stream directly from MultipartFile's InputStream
            clamAVService.scan(file.getInputStream());
            
        } catch (java.io.IOException e) {
            log.error("Failed to read file stream for scanning: {}", e.getMessage());
            throw new com.btctech.mailapp.exception.MailSecurityException("System error during security scanning.");
        }
    }

    /**
     * Validate user storage quota
     */
    public void validateQuota(Long accountId, long fileSize) {
        com.btctech.mailapp.entity.MailAccount account = mailAccountRepository.findById(accountId)
                .orElseThrow(() -> new com.btctech.mailapp.exception.MailException("Mail account not found"));

        log.debug("Quota Check for account {}: Used={}, Limit={}, Upload={}", 
                accountId, account.getStorageUsed(), account.getStorageLimit(), fileSize);

        if (account.getStorageUsed() + fileSize > account.getStorageLimit()) {
            log.error("Quota Exceeded for account {}: Limit={}, Required={}", 
                    accountId, account.getStorageLimit(), account.getStorageUsed() + fileSize);
            throw new com.btctech.mailapp.exception.MailException("Storage quota exceeded. Please upgrade your plan or delete some emails.");
        }
    }
}
