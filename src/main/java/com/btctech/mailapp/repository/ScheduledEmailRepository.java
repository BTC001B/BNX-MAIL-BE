package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.ScheduledEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, Long> {
    
    List<ScheduledEmail> findByUserEmailAndProcessedFalse(String userEmail);
    
    List<ScheduledEmail> findByProcessedFalseAndScheduledAtBefore(LocalDateTime time);
    
    Optional<ScheduledEmail> findByIdAndUserEmail(Long id, String userEmail);
}
