package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    
    Optional<UserSession> findByJwtToken(String jwtToken);
    
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :dateTime")
    int deleteByExpiresAtBefore(LocalDateTime dateTime);
    
    // Get all sessions for a user
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId")
    java.util.List<UserSession> findByUserId(Long userId);
}