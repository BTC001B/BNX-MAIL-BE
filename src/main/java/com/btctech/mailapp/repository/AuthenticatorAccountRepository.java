package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.AuthenticatorAccount;
import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuthenticatorAccountRepository extends JpaRepository<AuthenticatorAccount, Long> {
    List<AuthenticatorAccount> findByUser(User user);
    void deleteByUserIdAndAccountName(Long userId, String accountName);
}
