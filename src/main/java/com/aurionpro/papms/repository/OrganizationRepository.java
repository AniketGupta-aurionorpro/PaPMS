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
//for login and authentication purposes, username is a unique
    Optional<Organization> findByUsername(String username);
    
    List<Organization> findByStatus(OrganizationStatus status);
}