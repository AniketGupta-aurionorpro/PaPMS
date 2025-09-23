package com.aurionpro.papms.service;


import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.entity.Organization;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;


public interface OrganizationService {


//    Organization registerOrganizationWithDocuments(OrganizationRegistrationReq request, MultipartFile document1, MultipartFile document2);
    Organization registerOrganizationWithDocuments(String organizationDataJson, MultipartFile document1, MultipartFile document2);
    List<Organization> getAllOrganizations(); //bank ad /-

    List<Organization> getPendingOrganizations();
    
    Optional<Organization> getOrganizationById(Integer id);

    Optional<Organization> getOrganizationByName(String companyName);

    Optional<Organization> getOrganizationByUsername(String username);

    Organization approveOrganization(Integer id);

    Organization rejectOrganization(Integer id, String rejectionReason);

    Organization suspendOrganization(Integer id);

    OrganizationProfileResponse getProfile(Integer id);

//    List<Document> uploadVerificationDocuments(Integer organizationId, MultipartFile document1, MultipartFile document2);
    List<DocumentResponseDto> uploadVerificationDocuments(Integer organizationId, MultipartFile document1, MultipartFile document2);
}