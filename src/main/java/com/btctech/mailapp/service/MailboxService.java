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
import org.springframework.security.crypto.password.PasswordEncoder;
import java.io.File;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailboxService {
    
    private final MailAccountRepository mailAccountRepository;
    private final DomainRepository domainRepository;
    private final com.btctech.mailapp.repository.UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${mail.domain}")
    private String mailDomain;
    
    @Value("${mail.storage.base-path}")
    private String basePath; // ✅ READ FROM CONFIG - NO HARDCODED VALUE!
    
    /**
     * Create custom email with automatic mailbox creation
     */
    @Transactional
    public MailAccount createCustomEmail(User user, CreateEmailRequest request, String plainPassword, String overrideDomain) {
        try {
            log.info("Creating email for user: {}, email_name: {}, domain: {}", 
                user.getUsername(), request.getEmailName(), overrideDomain != null ? overrideDomain : mailDomain);
            
            // ✅ LOG THE BASE PATH BEING USED
            log.info("Using base path: {}", basePath);
            
            // Validate
            validateEmailName(request.getEmailName());
            
            // Determine domain to use
            String activeDomain = (overrideDomain != null) ? overrideDomain : mailDomain;

            // Get domain record (or handle business domain differently if it's not in 'domains' table yet)
            // For now, let's look it up or create a placeholder if it's a business domain
            Long domainId = 1L; // Default
            try {
                Domain domain = domainRepository.findByDomain(activeDomain)
                        .orElseGet(() -> {
                            Domain newDomain = new Domain();
                            newDomain.setDomain(activeDomain);
                            // Removing setCreatedBy as it's not in the entity
                            return domainRepository.save(newDomain);
                        });
                domainId = domain.getId();
            } catch (Exception e) {
                log.warn("Could not find/create Domain record for {}, using placeholder", activeDomain);
            }

            String fullEmail = request.getEmailName() + "@" + activeDomain;
            
            // Check if email exists
            if (mailAccountRepository.existsByEmail(fullEmail)) {
                throw new MailException("Email already exists: " + fullEmail);
            }
            
            // ✅ CONSTRUCT PATH USING basePath FROM CONFIG
            String maildirPath = basePath + "/" + activeDomain + "/" + request.getEmailName();
            
            log.info("Maildir path will be: {}", maildirPath);
            
            // Create mailbox on filesystem
            boolean created = createMailboxDirectory(request.getEmailName(), activeDomain);
            if (!created) {
                throw new MailException("Failed to create mailbox directory");
            }
            
            // Store HASHED password (for Postfix/Dovecot) with {BLF-CRYPT} prefix
            String storedPassword = "{BLF-CRYPT}" + passwordEncoder.encode(plainPassword);
            
            // Create database record
            MailAccount mailAccount = new MailAccount();
            mailAccount.setUserId(user.getId());
            mailAccount.setDomainId(domainId); // ✅ Using domainId from above
            mailAccount.setEmailName(request.getEmailName());
            mailAccount.setEmail(fullEmail);
            mailAccount.setMaildirPath(maildirPath); // ✅ USING CONSTRUCTED PATH
            mailAccount.setPassword(storedPassword);
            
            // Set Quota based on AccountType
            long limitInBytes = 1073741824L; // 1GB (Public)
            if (com.btctech.mailapp.entity.AccountType.BUSINESS.equals(user.getAccountType())) {
                limitInBytes = 53687091200L; // 50GB (Business)
            } else if (com.btctech.mailapp.entity.AccountType.CHILD.equals(user.getAccountType())) {
                limitInBytes = 524288000L; // 500MB (Child)
            }
            mailAccount.setStorageLimit(limitInBytes);
            mailAccount.setStorageUsed(0L);
            mailAccount.setQuota(limitInBytes); // Keeping legacy quota field in sync
            
            mailAccount.setIsPrimary(false);
            mailAccount.setActive(true);
            
            mailAccount = mailAccountRepository.save(mailAccount);
            log.info("✓ Mail account created: {} with path: {}", fullEmail, maildirPath);
            
            // ✅ AUTO-LINK: Update User entity with this email if it's their first one
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                user.setEmail(fullEmail);
                userRepository.save(user);
                log.info("✓ Linked primary email {} to user {}", fullEmail, user.getUsername());
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
    private boolean createMailboxDirectory(String emailName, String activeDomain) {
        try {
            // ✅ USE basePath FROM CONFIG
            String fullPath = basePath + "/" + activeDomain + "/" + emailName + "/Maildir";
            
            log.info("Creating mailbox directory: {}", fullPath);
            
            File maildirBase = new File(basePath + "/" + activeDomain + "/" + emailName);
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
            
            // Change ownership using system command (Linux only)
            String os = System.getProperty("os.name").toLowerCase();
            if (!os.contains("mac")) {
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
            } else {
                log.info("ℹ Skipping chown on Mac environment");
            }
            
            // Set permissions
            try {
                maildirBase.setReadable(true, false);
                maildirBase.setWritable(true, false);
                maildirBase.setExecutable(true, false);
                log.info("✓ Set directory permissions via Java API");
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

    public List<MailAccount> getAllEmails(String domain) {
        if (domain != null && !domain.isEmpty()) {
            return mailAccountRepository.findByEmailEndingWith("@" + domain);
        }
        return mailAccountRepository.findAll();
    }
}