package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.BlockedContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BlockedContactRepository extends JpaRepository<BlockedContact, Long> {
    List<BlockedContact> findByUserEmail(String userEmail);
    boolean existsByUserEmailAndBlockedEmail(String userEmail, String blockedEmail);
    Optional<BlockedContact> findByUserEmailAndBlockedEmail(String userEmail, String blockedEmail);
    void deleteByUserEmailAndBlockedEmail(String userEmail, String blockedEmail);
}
