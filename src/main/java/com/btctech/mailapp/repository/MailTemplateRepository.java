package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.MailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MailTemplateRepository extends JpaRepository<MailTemplate, Long> {
    List<MailTemplate> findByUserEmail(String userEmail);
    List<MailTemplate> findByUserEmailOrderByNameAsc(String userEmail);
}
