package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.BlockedSender;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Repository
public interface BlockedSenderRepository extends JpaRepository<BlockedSender, Long> {

    List<BlockedSender> findByUserEmail(String userEmail);

    boolean existsByUserEmailAndBlockedEmail(String userEmail, String blockedEmail);

    @Modifying
    @Transactional
    void deleteByUserEmailAndBlockedEmail(String userEmail, String blockedEmail);
}
