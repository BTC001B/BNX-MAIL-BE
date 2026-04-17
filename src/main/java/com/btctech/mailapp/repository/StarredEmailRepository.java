package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.StarredEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StarredEmailRepository extends JpaRepository<StarredEmail, Long> {
    
    List<StarredEmail> findByUserEmail(String userEmail);
    
    Optional<StarredEmail> findByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
    
    boolean existsByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
    
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByUserEmailAndUidAndFolderName(String userEmail, String uid, String folderName);
}
