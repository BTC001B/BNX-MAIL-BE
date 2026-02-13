package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DomainRepository extends JpaRepository<Domain, Long> {
    Optional<Domain> findByDomain(String domain);
}
