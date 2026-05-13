package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.ExternalAppSession;
import com.btctech.mailapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExternalAppSessionRepository extends JpaRepository<ExternalAppSession, Long> {
    List<ExternalAppSession> findByUserOrderByLoggedInAtDesc(User user);
}
