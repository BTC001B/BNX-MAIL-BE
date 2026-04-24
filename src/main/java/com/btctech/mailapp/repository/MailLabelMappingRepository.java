package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.MailLabelMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MailLabelMappingRepository extends JpaRepository<MailLabelMapping, Long> {
    List<MailLabelMapping> findByUserEmailAndEmailUidAndFolderName(String userEmail, String emailUid, String folderName);
    Optional<MailLabelMapping> findByUserEmailAndEmailUidAndFolderNameAndLabelId(String userEmail, String emailUid, String folderName, Long labelId);
    List<MailLabelMapping> findByLabelId(Long labelId);
    List<MailLabelMapping> findByUserEmailAndLabelId(String userEmail, Long labelId);
    void deleteByLabelId(Long labelId);
}
