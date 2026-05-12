package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.VerificationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationSessionRepository extends JpaRepository<VerificationSession, Long> {
    Optional<VerificationSession> findByReferenceId(String referenceId);
    Optional<VerificationSession> findByVerificationId(String verificationId);
    Optional<VerificationSession> findByMailAccountIdAndStatus(Long mailAccountId, String status);
}
