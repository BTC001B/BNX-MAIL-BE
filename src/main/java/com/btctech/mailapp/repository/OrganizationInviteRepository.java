package com.btctech.mailapp.repository;

import com.btctech.mailapp.entity.OrganizationInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationInviteRepository extends JpaRepository<OrganizationInvite, Long> {
    Optional<OrganizationInvite> findByInviteToken(String inviteToken);
    Optional<OrganizationInvite> findByEmailAndOrganizationId(String email, Long organizationId);
}
