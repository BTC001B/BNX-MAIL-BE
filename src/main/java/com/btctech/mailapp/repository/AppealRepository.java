package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Appeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppealRepository extends JpaRepository<Appeal, Long> {
    Optional<Appeal> findFirstByBannedUserIdOrderByCreatedAtDesc(Long bannedUserId);
}
