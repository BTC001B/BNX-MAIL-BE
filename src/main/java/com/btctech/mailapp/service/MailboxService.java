package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.CreateEmailRequest;
import com.btctech.mailapp.entity.Domain;
import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.DomainRepository;
import com.btctech.mailapp.repository.MailAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.File;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxService {
    
    private final MailAccountRepository mailAccountRepository;
    private final DomainRepository domainRepository;
    
    @Value("${mail.domain}")
    private String mailDomain;
    
    @Value("${mail.storage.base-path}")
    private String basePath; // ✅ READ FROM CONFIG - NO HARDCODED VALUE!
    
    /**
     * Create custom email with automatic mailbox creation
     */
    @Transactional
    public MailAccount createCustomEmail(User user, CreateEmailRequest request, String plainPassword) {
        try {
            log.info("Creating email for user: {}, email_name: {}", user.getUsername(), request.getEmailName());
            
            // ✅ LOG THE BASE PATH BEING USED
            log.info("Using base path: {}", basePath);
            
            // Validate
            validateEmailName(request.getEmailName());
            
            // Get domain
            Domain domain = domainRepository.findByDomain(mailDomain)
                    .orElseThrow(() -> new MailException("Domain not found: " + mailDomain));
            
            String fullEmail = request.getEmailName() + "@" + mailDomain;
            
            // Check if email exists
            if (mailAccountRepository.existsByEmail(fullEmail)) {
                throw new MailException("Email already exists: " + fullEmail);
            }
            
            // ✅ CONSTRUCT PATH USING basePath FROM CONFIG
            String maildirPath = basePath + "/" + mailDomain + "/" + request.getEmailName();
            
            log.info("Maildir path will be: {}", maildirPath);
            
            // Create mailbox on filesystem
            boolean created = createMailboxDirectory(request.getEmailName());
            if (!created) {
                throw new MailException("Failed to create mailbox directory");
            }
            
            // Store PLAIN password (for Postfix/Dovecot)
            String storedPassword = plainPassword;
            
            // Create database record
            MailAccount mailAccount = new MailAccount();
            mailAccount.setUserId(user.getId());
            mailAccount.setDomainId(domain.getId());
            mailAccount.setEmailName(request.getEmailName());
            mailAccount.setEmail(fullEmail);
            mailAccount.setMaildirPath(maildirPath); // ✅ USING CONSTRUCTED PATH
            mailAccount.setPassword(storedPassword);
            mailAccount.setQuota(0L);
            mailAccount.setIsPrimary(false);
            mailAccount.setActive(true);
            
            mailAccount = mailAccountRepository.save(mailAccount);
            log.info("✓ Mail account created: {} with path: {}", fullEmail, maildirPath);
            
            // Set as primary if first email
            List<MailAccount> userEmails = mailAccountRepository.findByUserId(user.getId());
            if (userEmails.size() == 1) {
                mailAccount.setIsPrimary(true);
                mailAccountRepository.save(mailAccount);
                log.info("✓ Set as primary email: {}", fullEmail);
            }
            
            return mailAccount;
            
        } catch (MailException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create email: {}", e.getMessage(), e);
            throw new MailException("Failed to create email: " + e.getMessage());
        }
    }
    
    /**
     * Create mailbox directory structure
     */
    private boolean createMailboxDirectory(String emailName) {
        try {
            // ✅ USE basePath FROM CONFIG
            String fullPath = basePath + "/" + mailDomain + "/" + emailName + "/Maildir";
            
            log.info("Creating mailbox directory: {}", fullPath);
            
            File maildirBase = new File(basePath + "/" + mailDomain + "/" + emailName);
            File maildir = new File(fullPath);
            File newDir = new File(maildir, "new");
            File curDir = new File(maildir, "cur");
            File tmpDir = new File(maildir, "tmp");
            
            // Create directories
            newDir.mkdirs();
            curDir.mkdirs();
            tmpDir.mkdirs();
            
            // Create subfolders
            String[] folders = {"Sent", "Drafts", "Trash", "Spam", "Archive"};
            for (String folder : folders) {
                new File(maildir, "." + folder + "/new").mkdirs();
                new File(maildir, "." + folder + "/cur").mkdirs();
                new File(maildir, "." + folder + "/tmp").mkdirs();
            }
            
            // Change ownership using system command
            try {
                String[] chownCmd = {"chown", "-R", "vmail:vmail", maildirBase.getAbsolutePath()};
                Process process = Runtime.getRuntime().exec(chownCmd);
                int exitCode = process.waitFor();
                
                if (exitCode == 0) {
                    log.info("✓ Ownership changed to vmail:vmail");
                } else {
                    log.warn("⚠ Could not change ownership (exit code: {})", exitCode);
                }
            } catch (Exception e) {
                log.warn("⚠ Could not change ownership: {}", e.getMessage());
            }
            
            // Set permissions
            try {
                String[] chmodCmd = {"chmod", "-R", "700", maildirBase.getAbsolutePath()};
                Process process = Runtime.getRuntime().exec(chmodCmd);
                process.waitFor();
            } catch (Exception e) {
                log.warn("⚠ Could not set permissions: {}", e.getMessage());
            }
            
            log.info("✓ Mailbox directory created: {}", fullPath);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to create mailbox directory: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Validate email name
     */
    private void validateEmailName(String emailName) {
        if (emailName == null || emailName.isEmpty()) {
            throw new MailException("Email name is required");
        }
        
        if (!emailName.equals(emailName.toLowerCase())) {
            throw new MailException("Email name must be lowercase");
        }
        
        if (!emailName.matches("^[a-z0-9._-]+$")) {
            throw new MailException("Email name can only contain lowercase letters, numbers, dots, hyphens, underscores");
        }
        
        if (emailName.length() < 3 || emailName.length() > 30) {
            throw new MailException("Email name must be between 3-30 characters");
        }
    }
    
    // Other methods...
    public List<MailAccount> getUserEmails(Long userId) {
        return mailAccountRepository.findByUserId(userId);
    }
    
    public MailAccount getPrimaryEmail(Long userId) {
        return mailAccountRepository.findByUserIdAndIsPrimary(userId, true)
                .orElseThrow(() -> new MailException("No primary email found"));
    }
    
    public MailAccount getMailAccountByEmail(String email) {
        return mailAccountRepository.findByEmail(email)
                .orElseThrow(() -> new MailException("Mail account not found: " + email));
    }
    
    @Transactional
    public void setPrimaryEmail(Long userId, Long mailAccountId) {
        List<MailAccount> accounts = mailAccountRepository.findByUserId(userId);
        
        for (MailAccount account : accounts) {
            account.setIsPrimary(false);
            mailAccountRepository.save(account);
        }
        
        MailAccount primary = mailAccountRepository.findById(mailAccountId)
                .orElseThrow(() -> new MailException("Mail account not found"));
        
        if (!primary.getUserId().equals(userId)) {
            throw new MailException("Unauthorized");
        }
        
        primary.setIsPrimary(true);
        mailAccountRepository.save(primary);
        
        log.info("✓ Set primary email: {} for user: {}", primary.getEmail(), userId);
    }
}