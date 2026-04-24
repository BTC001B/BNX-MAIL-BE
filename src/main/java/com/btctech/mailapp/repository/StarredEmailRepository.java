package com.btctech.mailapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Repository
public interface StarredEmailRepository extends JpaRepository<com.btctech.mailapp.entity.StarredEmail, Long> {
    
    List<com.btctech.mailapp.entity.StarredEmail> findByUserEmail(String userEmail);
    
    Optional<com.btctech.mailapp.entity.StarredEmail> findByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
    
    boolean existsByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
    
    @Modifying
    @Transactional
    void deleteByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
}



