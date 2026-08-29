package com.btctech.mailapp.service;

import com.btctech.mailapp.dto.cashfree.CashfreeCreateUrlResponse;
import com.btctech.mailapp.dto.cashfree.CashfreeStatusResponse;
import com.btctech.mailapp.entity.VerificationSession;
import com.btctech.mailapp.repository.VerificationSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationService {

    private final CashfreeService cashfreeService;
    private final VerificationSessionRepository sessionRepository;
    private final MailboxService mailboxService;

    @Value("${app.frontend.redirect-url:https://www.b2auth.com/}")
    private String frontendRedirectUrl;

    @Transactional
    public String initiateVerification(Long userId, Long mailAccountId) {
        String referenceId = "VER_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        CashfreeCreateUrlResponse cfResponse = cashfreeService.createDigiLockerUrl(referenceId, frontendRedirectUrl);

        VerificationSession session = new VerificationSession();
        session.setUserId(userId);
        session.setMailAccountId(mailAccountId);
        session.setReferenceId(referenceId);
        session.setVerificationId(cfResponse.getVerificationId());
        session.setStatus("PENDING");
        sessionRepository.save(session);

        log.info("Initiated verification for mailAccountId: {} with reference: {}", mailAccountId, referenceId);
        return cfResponse.getRedirectUrl();
    }

    @Transactional
    public String checkAndFinalizeVerification(String referenceId) {
        VerificationSession session = sessionRepository.findByReferenceId(referenceId)
                .orElseThrow(() -> new RuntimeException("Verification session not found"));

        // Use case-insensitive check for existing status
        String currentStatus = session.getStatus() != null ? session.getStatus().toUpperCase() : "";
        if (currentStatus.equals("AUTHENTICATED") || currentStatus.equals("VERIFIED") || currentStatus.equals("SUCCESS")) {
            return "SUCCESS";
        }

        CashfreeStatusResponse cfStatus = cashfreeService.getVerificationStatus(session.getVerificationId());
        
        String cfStatusValue = cfStatus.getVerificationStatus();
        String cfReqStatus = cfStatus.getStatus();
        
        log.info("Cashfree status check for {}: verification_status={}, status={}", 
            referenceId, cfStatusValue, cfReqStatus);
        
        String finalStatus = (cfStatusValue != null) ? cfStatusValue : cfReqStatus;
        if (finalStatus == null) finalStatus = "PENDING";

        session.setStatus(finalStatus);
        sessionRepository.saveAndFlush(session);

        String upperStatus = finalStatus.toUpperCase();
        if (upperStatus.equals("AUTHENTICATED") || upperStatus.equals("VERIFIED") || upperStatus.equals("SUCCESS")) {
            finalizePromotion(session);
            return "SUCCESS";
        }

        return finalStatus;
    }

    private void finalizePromotion(VerificationSession session) {
        log.info("Promoting email account {} to primary for user {}", session.getMailAccountId(), session.getUserId());
        mailboxService.setPrimaryEmail(session.getUserId(), session.getMailAccountId());
    }

    @Transactional
    public boolean verifyPanAndFinalize(Long userId, Long mailAccountId, String pan, String name) {
        log.info("Initiating PAN verification for user {} and mailAccountId {}", userId, mailAccountId);
        
        com.btctech.mailapp.dto.cashfree.CashfreePanResponse cfResponse = cashfreeService.verifyPan(pan, name);
        
        if (cfResponse.isValid()) {
            log.info("PAN Verification SUCCESS for user {}. Promoting email...", userId);
            mailboxService.setPrimaryEmail(userId, mailAccountId);
            return true;
        } else {
            log.warn("PAN Verification FAILED for user {}. Reason: {}", userId, cfResponse.getMessage());
            return false;
        }
    }
}
