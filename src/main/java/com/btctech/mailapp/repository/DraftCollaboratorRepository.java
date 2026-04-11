package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.DraftCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DraftCollaboratorRepository extends JpaRepository<DraftCollaborator, Long> {
    Optional<DraftCollaborator> findByDraftIdAndUserId(Long draftId, Long userId);
    List<DraftCollaborator> findByDraftId(Long draftId);
}
