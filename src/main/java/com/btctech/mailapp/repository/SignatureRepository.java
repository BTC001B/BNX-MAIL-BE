package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByUserId(Long userId);
    List<Signature> findByUserIdAndIsDefaultTrue(Long userId);
}
