package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.BusinessProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessProfileRepository extends JpaRepository<BusinessProfile, Long> {
    java.util.Optional<BusinessProfile> findByUserId(Long userId);
    boolean existsByCin(String cin);
    boolean existsByGstin(String gstin);
}
