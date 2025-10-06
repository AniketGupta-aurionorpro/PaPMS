package com.aurionpro.papms.controller;

import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.service.OrganizationService;
import com.aurionpro.papms.service.OrganizationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;
import com.aurionpro.papms.mapper.OrganizationMapper;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;

    // Endpoint for self-registration of a new organization
//    @PostMapping("/register")
//    public ResponseEntity<Organization> registerOrganization(@Valid @RequestBody OrganizationRegistrationReq request) {
//        Organization newOrg = organizationService.registerOrganization(request);
//        return new ResponseEntity<>(newOrg, HttpStatus.CREATED);
//    }

    @Operation(
            summary = "Register a new organization with verification documents and an optional logo", // MODIFIED
            description = "This endpoint registers a new organization. Provide organization data as a JSON string, two PDF documents, and an optional logo image.", // MODIFIED
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", implementation = OrganizationRegistrationMultipart.class)
                    )
            )
    )
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<OrganizationResponseDto> registerOrganizationWithDocuments(
            @RequestPart("organizationData") String organizationDataJson,
            @RequestPart("document1") MultipartFile document1,
            @RequestPart("document2") MultipartFile document2,
            @RequestPart(value = "logo", required = false) MultipartFile logo) { // MODIFIED: Added optional logo part

        // Pass the new logo file to the service
        Organization newOrg = organizationService.registerOrganizationWithDocuments(organizationDataJson, document1, document2, logo); // MODIFIED
        return new ResponseEntity<>(OrganizationMapper.toDto(newOrg), HttpStatus.CREATED);
    }

    private static class OrganizationRegistrationMultipart {
        @Schema(type = "string", description = "The organization's details in JSON format.")
        public OrganizationRegistrationReq organizationData;

        @Schema(description = "The first verification document (PDF).", type = "string", format = "binary")
        public MultipartFile document1;

        @Schema(description = "The second verification document (PDF).", type = "string", format = "binary")
        public MultipartFile document2;

        @Schema(description = "The organization's logo (JPG, JPEG, PNG).", type = "string", format = "binary")
        public MultipartFile logo; // ADD THIS
    }



    // Endpoint to get all organizations
    @GetMapping
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Transactional(readOnly = true)

    public ResponseEntity<Page<OrganizationResponseDto>> getAllOrganizations(@ParameterObject Pageable pageable) {
        // The controller calls the service. That's it.
        Page<OrganizationResponseDto> organizationsPage = organizationService.getAllOrganizations(pageable);
        return ResponseEntity.ok(organizationsPage);
    }
//    public ResponseEntity<List<OrganizationResponseDto>> getAllOrganizations() {
//        List<Organization> organizations = organizationService.getAllOrganizations();
//        List<OrganizationResponseDto> responseDtos = organizations.stream()
//                .map(OrganizationMapper::toSimpleDto) // without nested collections
//                .collect(Collectors.toList());
//        return ResponseEntity.ok(responseDtos);
//    }



    // Endpoint to get all pending organizations
    @GetMapping("/pending")
   // @PreAuthorize("hasAuthority('BANK_ADMIN')")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<List<Organization>> getPendingOrganizations() {
        List<Organization> pendingOrgs = organizationService.getPendingOrganizations();
        return ResponseEntity.ok(pendingOrgs);
    }

    // Endpoint to get an organization by its ID
//    @GetMapping("/{id}")
//    //@PreAuthorize("hasAnyAuthority('BANK_ADMIN', 'ORG_ADMIN')")
//    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
//    public ResponseEntity<Organization> getOrganizationById(@PathVariable Integer id) {
//        return organizationService.getOrganizationById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

    // Endpoint to get an organization by its company name (Search)
    @GetMapping("/by-name/{companyName}")
   @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationResponseDtowithEmployee> getOrganizationByName(@PathVariable String companyName) {
//        ResponseEntity<Organization> ORG = organizationService.getOrganizationByName(companyName)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
        return organizationService.getOrganizationByName(companyName)
                .map(org -> ResponseEntity.ok(OrganizationMapper.toDtoWithEmployees(org)))
                .orElse(ResponseEntity.notFound().build());
    }

    // Endpoint to get an organization by its username (Search)
//    @GetMapping("/by-username/{username}")
//    //@PreAuthorize("hasAnyAuthority('BANK_ADMIN')")
//    @PreAuthorize("hasRole('BANK_ADMIN')")
//    public ResponseEntity<Organization> getOrganizationByUsername(@PathVariable String username) {
//        return organizationService.getOrganizationByUsername(username)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }

    // Endpoint for Bank Admin to approve a pending organization
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> approveOrganization(@PathVariable Integer id) {
        Organization approvedOrg = organizationService.approveOrganization(id);
        OrganizationResponseDto responseDto = OrganizationMapper.toDto(approvedOrg);
        return ResponseEntity.ok(responseDto);
    }

    // Endpoint for Bank Admin to reject an organization
    @PutMapping("/{id}/reject")
    //@PreAuthorize("hasAuthority('BANK_ADMIN')")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<Organization> rejectOrganization(@PathVariable Integer id, @RequestBody String rejectionReason) {
        Organization rejectedOrg = organizationService.rejectOrganization(id, rejectionReason);
        return ResponseEntity.ok(rejectedOrg);
    }

    // Endpoint for Bank Admin to suspend an organization
    @PutMapping("/{id}/suspend")
    //@PreAuthorize("hasAuthority('BANK_ADMIN')")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<Organization> suspendOrganization(@PathVariable Integer id) {
        Organization suspendedOrg = organizationService.suspendOrganization(id);
        return ResponseEntity.ok(suspendedOrg);
    }

    // Endpoint to get an organization's profile
    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationProfileResponse> getProfile(@PathVariable Integer id) {
        OrganizationProfileResponse profile = organizationService.getProfile(id);
        return ResponseEntity.ok(profile);
    }

//    @PostMapping("/{id}/upload-verification-documents")
//    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
//    public ResponseEntity<List<DocumentResponseDto>> uploadDocuments(
//            @PathVariable Integer id,
//            @RequestParam("document1") MultipartFile document1,
//            @RequestParam("document2") MultipartFile document2) {
//        List<DocumentResponseDto> savedDocumentsDto = organizationService.uploadVerificationDocuments(id, document1, document2);
//        return ResponseEntity.status(HttpStatus.CREATED).body(savedDocumentsDto);
//    }
}