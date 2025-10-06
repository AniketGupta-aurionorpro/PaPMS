package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.entity.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {

    
    Optional<Organization> findByCompanyName(String companyName);

    
    //List<Organization> findByStatus(OrganizationStatus status);

    // MODIFIED: This now returns a Page instead of a List
    Page<Organization> findByStatus(OrganizationStatus status, Pageable pageable);

    // ADD THIS NEW METHOD: A non-paginated method to get all pending organizations
    List<Organization> findAllByStatus(OrganizationStatus status);

    Optional<Organization> findByBankAssignedAccountNumber(java.lang.String newAccountNumber);
}