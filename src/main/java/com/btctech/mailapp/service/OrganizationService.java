package com.btctech.mailapp.service;

import com.btctech.mailapp.entity.Organization;
import com.btctech.mailapp.exception.MailException;
import com.btctech.mailapp.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Transactional
    public Organization createOrganization(String name, String domain) {
        if (organizationRepository.existsByDomain(domain)) {
            throw new MailException("Organization with domain " + domain + " already exists");
        }

        Organization organization = new Organization();
        organization.setName(name);
        organization.setDomain(domain);
        organization.setVerified(false);

        organization = organizationRepository.save(organization);
        log.info("Created organization: {} for domain: {}", name, domain);
        
        return organization;
    }

    public Organization getByDomain(String domain) {
        return organizationRepository.findByDomain(domain)
                .orElseThrow(() -> new MailException("Organization not found for domain: " + domain));
    }
}
