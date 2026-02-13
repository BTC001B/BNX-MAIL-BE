package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.MailAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface MailAccountRepository extends JpaRepository<MailAccount, Long> {
    Optional<MailAccount> findByEmail(String email);

    List<MailAccount> findByUserId(Long userId);

    Optional<MailAccount> findByUserIdAndIsPrimary(Long userId, Boolean isPrimary);

    boolean existsByEmail(String email);
}
