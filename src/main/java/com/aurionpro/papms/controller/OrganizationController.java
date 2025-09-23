package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.service.OrganizationService;
import com.aurionpro.papms.service.OrganizationServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
            summary = "Register a new organization with verification documents",
            description = "This endpoint registers a new organization. Provide organization data as a JSON string and two PDF documents.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "multipart/form-data",
                            schema = @Schema(type = "object", implementation = OrganizationRegistrationMultipart.class)
                    )
            )
    )
    @PostMapping(value = "/register", consumes = "multipart/form-data")
    public ResponseEntity<Organization> registerOrganizationWithDocuments(
            @RequestPart("organizationData") String organizationDataJson, // <-- CHANGE: Receive as String
            @RequestPart("document1") MultipartFile document1,
            @RequestPart("document2") MultipartFile document2) {

        // CHANGE: Pass the JSON string directly to the service
        Organization newOrg = organizationService.registerOrganizationWithDocuments(organizationDataJson, document1, document2);
        return new ResponseEntity<>(newOrg, HttpStatus.CREATED);
    }

    private static class OrganizationRegistrationMultipart {
        // This class is for Swagger documentation purposes and is fine as is.
        @Schema(type = "string", description = "The organization's details in JSON format. IMPORTANT: This part must have a Content-Type of application/json.")
        public OrganizationRegistrationReq organizationData;

        @Schema(description = "The first verification document (PDF).", type = "string", format = "binary")
        public MultipartFile document1;

        @Schema(description = "The second verification document (PDF).", type = "string", format = "binary")
        public MultipartFile document2;
    }


    // Endpoint to get all organizations
    @GetMapping
    //@PreAuthorize("hasAuthority('BANK_ADMIN')")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<List<Organization>> getAllOrganizations() {
        List<Organization> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

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
   // @PreAuthorize("hasAnyAuthority('BANK_ADMIN', 'ORG_ADMIN')")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Organization> getOrganizationByName(@PathVariable String companyName) {
        return organizationService.getOrganizationByName(companyName)
                .map(ResponseEntity::ok)
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
    //@PreAuthorize("hasAuthority('BANK_ADMIN')")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<Organization> approveOrganization(@PathVariable Integer id) {
        Organization approvedOrg = organizationService.approveOrganization(id);
        return ResponseEntity.ok(approvedOrg);
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
   // @PreAuthorize("hasAnyAuthority('BANK_ADMIN', 'ORG_ADMIN')")
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