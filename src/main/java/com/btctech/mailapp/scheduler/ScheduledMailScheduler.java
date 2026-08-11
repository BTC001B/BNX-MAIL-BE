package com.btctech.mailapp.scheduler;

import com.btctech.mailapp.entity.MailAccount;
import com.btctech.mailapp.entity.ScheduledEmail;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.dto.SendMailRequest;
import com.btctech.mailapp.dto.AttachmentInfo;
import com.btctech.mailapp.repository.ScheduledEmailRepository;
import com.btctech.mailapp.service.MailSendService;
import com.btctech.mailapp.service.MailboxService;
import com.btctech.mailapp.service.SessionService;
import com.btctech.mailapp.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledMailScheduler {

    private final ScheduledEmailRepository scheduledEmailRepository;
    private final UserService userService;
    private final SessionService sessionService;
    private final MailboxService mailboxService;
    private final MailSendService mailSendService;
    private final ObjectMapper objectMapper;

    /**
     * Every minute, check for scheduled emails that should be sent.
     */
    @Scheduled(fixedRate = 60000)
    public void processScheduledEmails() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledEmail> pendingEmails = scheduledEmailRepository.findByProcessedFalseAndScheduledAtBefore(now);

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Found {} scheduled emails to send.", pendingEmails.size());

        for (ScheduledEmail scheduledEmail : pendingEmails) {
            try {
                processSend(scheduledEmail);
            } catch (Exception e) {
                log.error("Failed to send scheduled email {} for {}: {}", scheduledEmail.getId(), scheduledEmail.getUserEmail(), e.getMessage());
            }
        }
    }

    private void processSend(ScheduledEmail scheduledEmail) {
        String fromEmail = scheduledEmail.getUserEmail();

        // 1. Get user to get password via session
        User user = userService.getUserByEmailOrUsername(fromEmail);
        String password = null;
        if (user != null) {
            password = sessionService.getPasswordByUserId(user.getId());
        }

        // 2. FALLBACK: If no active session, try persistent encrypted password
        if (password == null) {
            log.info("No session found for {}, trying persistent encrypted password for scheduled mail", fromEmail);
            try {
                MailAccount mailAccount = mailboxService.getMailAccountByEmail(fromEmail);
                if (mailAccount != null && mailAccount.getEncryptedPassword() != null) {
                    password = sessionService.decrypt(mailAccount.getEncryptedPassword());
                    log.info("✓ Recovered password from persistent storage for scheduled mail: {}", fromEmail);
                }
            } catch (Exception e) {
                log.error("Failed to decrypt persistent password: {}", e.getMessage());
            }
        }

        if (password == null) {
            if (fromEmail.equals("calendar@bnxmail.com") || fromEmail.equals("beta@bnxmail.com")) {
                log.info("Bypassing password requirement for public scheduled email account: {}", fromEmail);
                // mailSendService handles null passwords safely for these public accounts
            } else {
                log.warn("Cannot send scheduled email for {}: No password source found.", fromEmail);
                return;
            }
        }

        // 3. Build SendMailRequest
        SendMailRequest request = new SendMailRequest();
        request.setTo(scheduledEmail.getToRecipient());
        request.setCc(scheduledEmail.getCc());
        request.setBcc(scheduledEmail.getBcc());
        request.setSubject(scheduledEmail.getSubject());
        request.setBody(scheduledEmail.getBody());
        request.setFromName(scheduledEmail.getFromName());
        request.setIsHtml(scheduledEmail.getIsHtml());

        if (scheduledEmail.getAttachmentsJson() != null && !scheduledEmail.getAttachmentsJson().isEmpty()) {
            try {
                List<AttachmentInfo> attachments = objectMapper.readValue(
                        scheduledEmail.getAttachmentsJson(),
                        new TypeReference<List<AttachmentInfo>>() {}
                );
                request.setAttachments(attachments);
            } catch (Exception e) {
                log.error("Failed to parse attachments JSON for scheduled email {}: {}", scheduledEmail.getId(), e.getMessage());
            }
        }

        // 4. Send email
        mailSendService.sendMail(fromEmail, password, request);

        // 5. Mark as processed
        scheduledEmail.setProcessed(true);
        scheduledEmailRepository.save(scheduledEmail);

        log.info("✓ Successfully sent scheduled email {} for {}", scheduledEmail.getId(), fromEmail);
    }
}
