package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.PasswordResetToken;
import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByUserAndToken(User user, String token);
    void deleteByUser(User user);
}
