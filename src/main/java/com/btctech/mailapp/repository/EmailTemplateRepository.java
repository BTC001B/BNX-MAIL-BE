package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    List<EmailTemplate> findByUserEmail(String userEmail);
    Optional<EmailTemplate> findByIdAndUserEmail(Long id, String userEmail);
}
