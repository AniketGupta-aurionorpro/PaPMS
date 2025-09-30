package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {

    
    Optional<Organization> findByCompanyName(String companyName);

    
    List<Organization> findByStatus(OrganizationStatus status);

    Optional<Organization> findByBankAssignedAccountNumber(java.lang.String newAccountNumber);
}