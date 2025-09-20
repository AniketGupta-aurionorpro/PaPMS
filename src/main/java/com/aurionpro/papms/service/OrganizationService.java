package com.aurionpro.papms.service;


import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.entity.Organization;
import java.util.List;
import java.util.Optional;


public interface OrganizationService {

    
    Organization registerOrganization(OrganizationRegistrationReq request);
    
    List<Organization> getAllOrganizations(); //bank ad /-

    List<Organization> getPendingOrganizations();
    
    Optional<Organization> getOrganizationById(Integer id);

    Optional<Organization> getOrganizationByName(String companyName);

    Optional<Organization> getOrganizationByUsername(String username);

    Organization approveOrganization(Integer id);

    Organization rejectOrganization(Integer id, String rejectionReason);

    Organization suspendOrganization(Integer id);

    OrganizationProfileResponse getProfile(Integer id);
}