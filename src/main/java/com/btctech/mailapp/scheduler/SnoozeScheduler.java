package com.btctech.mailapp.scheduler;

import com.btctech.mailapp.entity.SnoozedEmail;
import com.btctech.mailapp.entity.User;
import com.btctech.mailapp.repository.SnoozedEmailRepository;
import com.btctech.mailapp.repository.UserRepository;
import com.btctech.mailapp.service.MailReceiveService;
import com.btctech.mailapp.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SnoozeScheduler {

    private final SnoozedEmailRepository snoozedEmailRepository;
    private final UserRepository userRepository;
    private final SessionService sessionService;
    private final MailReceiveService mailReceiveService;

    /**
     * Every minute, check for snoozed emails that should wake up.
     */
    @Scheduled(fixedRate = 60000)
    public void processSnoozedEmails() {
        LocalDateTime now = LocalDateTime.now();
        List<SnoozedEmail> pendingWakeups = snoozedEmailRepository.findByProcessedFalseAndWakeUpAtBefore(now);

        if (pendingWakeups.isEmpty()) {
            return;
        }

        log.info("Found {} snoozed emails to wake up.", pendingWakeups.size());

        for (SnoozedEmail snooze : pendingWakeups) {
            try {
                processWakeUp(snooze);
            } catch (Exception e) {
                log.error("Failed to wake up email {} for {}: {}", snooze.getUid(), snooze.getUserEmail(), e.getMessage());
            }
        }
    }

    private void processWakeUp(SnoozedEmail snooze) {
        String email = snooze.getUserEmail();
        
        // 1. Find user to get password via session
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.warn("Cannot wake up email: User {} not found.", email);
            return;
        }

        // 2. Get password from session
        String password = sessionService.getPasswordByUserId(user.getId());
        if (password == null) {
            log.warn("Cannot wake up email for {}: No active session found to retrieve IMAP password.", email);
            return;
        }

        // 3. Move message back to original folder (usually INBOX)
        mailReceiveService.moveMessage(email, password, "Snoozed", snooze.getUid(), snooze.getOriginalFolderName());

        // 4. Mark as processed
        snooze.setProcessed(true);
        snoozedEmailRepository.save(snooze);

        log.info("✓ Woke up email {} for {} and moved back to {}", snooze.getUid(), email, snooze.getOriginalFolderName());
    }
}
