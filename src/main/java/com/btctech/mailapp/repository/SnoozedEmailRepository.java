package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.SnoozedEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SnoozedEmailRepository extends JpaRepository<SnoozedEmail, Long> {
    
    List<SnoozedEmail> findByUserEmailAndProcessedFalse(String userEmail);
    
    List<SnoozedEmail> findByProcessedFalseAndWakeUpAtBefore(LocalDateTime time);
    
    Optional<SnoozedEmail> findByUserEmailAndUidAndOriginalFolderName(String userEmail, String uid, String originalFolderName);
}
