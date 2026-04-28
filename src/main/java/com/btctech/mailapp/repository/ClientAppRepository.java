package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.ClientApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ClientAppRepository extends JpaRepository<ClientApp, Long> {
    Optional<ClientApp> findByClientId(String clientId);
}
