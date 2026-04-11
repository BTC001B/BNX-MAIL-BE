package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.MailDraft;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MailDraftRepository extends JpaRepository<MailDraft, Long> {
    List<MailDraft> findByMailAccountIdOrderByUpdatedAtDesc(Long mailAccountId);
    java.util.Optional<MailDraft> findByIdAndMailAccountId(Long id, Long mailAccountId);
}
