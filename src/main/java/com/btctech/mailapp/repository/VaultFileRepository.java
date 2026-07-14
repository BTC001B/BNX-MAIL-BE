package com.btctech.mailapp.repository;

import com.btctech.mailapp.model.VaultFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VaultFileRepository extends JpaRepository<VaultFile, Long> {

    interface VaultFileMetadata {
        Long getId();
        String getFilename();
        String getContentType();
        Long getSize();
        String getOwnerEmail();
        LocalDateTime getUploadedAt();
    }

    List<VaultFileMetadata> findByOwnerEmailOrderByUploadedAtDesc(String ownerEmail);
}
