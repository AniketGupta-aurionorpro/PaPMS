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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrganizationController {

    private final OrganizationService organizationService;

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
        log.info("Received request to register organization with documents. Logo present: {}", (logo != null && !logo.isEmpty()));
        Organization newOrg = organizationService.registerOrganizationWithDocuments(organizationDataJson, document1, document2, logo); // MODIFIED
        log.info("Organization registered successfully with ID: {}. Status: PENDING_APPROVAL", newOrg.getId());
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

    @GetMapping
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<OrganizationResponseDto>> getAllOrganizations(@ParameterObject Pageable pageable) {
        log.info("Request to get all organizations with pagination: {}", pageable);
        Page<OrganizationResponseDto> organizationsPage = organizationService.getAllOrganizations(pageable);
        log.info("Returning {} organizations on page {}", organizationsPage.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(organizationsPage);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<List<Organization>> getPendingOrganizations() {
        log.info("Request to get all PENDING organizations.");
        List<Organization> pendingOrgs = organizationService.getPendingOrganizations();
        log.info("Found {} pending organizations.", pendingOrgs.size());
        return ResponseEntity.ok(pendingOrgs);
    }

    @GetMapping("/by-name/{companyName}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationResponseDtowithEmployee> getOrganizationByName(@PathVariable String companyName) {
        log.info("Request to get organization by company name: {}", companyName);
        return organizationService.getOrganizationByName(companyName)
                .map(org -> {
                    log.info("Found organization ID {} for name {}", org.getId(), companyName);
                    return ResponseEntity.ok(OrganizationMapper.toDtoWithEmployees(org));
                })
                .orElseGet(() -> {
                    log.warn("Organization with name '{}' not found.", companyName);
                    return ResponseEntity.notFound().build();
                });
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<OrganizationResponseDto> approveOrganization(@PathVariable Integer id) {
        log.info("Bank Admin request to APPROVE organization ID: {}", id);
        Organization approvedOrg = organizationService.approveOrganization(id);
        OrganizationResponseDto responseDto = OrganizationMapper.toDto(approvedOrg);
        log.info("Successfully approved organization ID {}. New status: ACTIVE", id);
        return ResponseEntity.ok(responseDto);
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<Organization> rejectOrganization(@PathVariable Integer id, @RequestBody String rejectionReason) {
        log.info("Bank Admin request to REJECT organization ID: {} with reason.", id);
        Organization rejectedOrg = organizationService.rejectOrganization(id, rejectionReason);
        log.info("Successfully rejected organization ID {}. New status: REJECTED", id);
        return ResponseEntity.ok(rejectedOrg);
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    public ResponseEntity<Organization> suspendOrganization(@PathVariable Integer id) {
        log.info("Bank Admin request to SUSPEND organization ID: {}", id);
        Organization suspendedOrg = organizationService.suspendOrganization(id);
        log.info("Successfully suspended organization ID {}. New status: SUSPENDED", id);
        return ResponseEntity.ok(suspendedOrg);
    }

    @GetMapping("/{id}/profile")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<OrganizationProfileResponse> getProfile(@PathVariable Integer id) {
        log.info("Request to get profile for organization ID: {}", id);
        OrganizationProfileResponse profile = organizationService.getProfile(id);
        return ResponseEntity.ok(profile);
    }
}