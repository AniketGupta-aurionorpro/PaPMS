package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.ConcernResponseDto;
import com.aurionpro.papms.dto.RaiseConcernRequest;
import com.aurionpro.papms.dto.UpdateConcernStatusRequest;
import com.aurionpro.papms.service.ConcernService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Concern Management", description = "APIs for employees to raise concerns and admins to manage them")
public class ConcernController {

    private final ConcernService concernService;

    // Endpoint for an Employee to raise a new concern
    @PostMapping("/concerns")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<ConcernResponseDto> raiseConcern(@Valid @RequestBody RaiseConcernRequest request) {
        ConcernResponseDto response = concernService.raiseConcern(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // Endpoint for an Employee to view all of their own concerns
    @GetMapping("/concerns/my-concerns")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<ConcernResponseDto>> getMyConcerns() {
        return ResponseEntity.ok(concernService.getMyConcerns());
    }

    // Endpoint for an Org Admin to view all concerns in their organization
    @GetMapping("/organizations/{organizationId}/concerns")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<List<ConcernResponseDto>> getConcernsForOrganization(@PathVariable Integer organizationId) {
        return ResponseEntity.ok(concernService.getConcernsForOrganization(organizationId));
    }

    // Endpoint for both Employee and Org Admin to view a specific concern
    @GetMapping("/concerns/{concernId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ORG_ADMIN')")
    public ResponseEntity<ConcernResponseDto> getConcernById(@PathVariable Long concernId) {
        return ResponseEntity.ok(concernService.getConcernById(concernId));
    }

    // Endpoint for an Org Admin to update the status of a concern
    @PutMapping("/concerns/{concernId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ConcernResponseDto> updateConcernStatus(
            @PathVariable Long concernId,
            @Valid @RequestBody UpdateConcernStatusRequest request) {
        return ResponseEntity.ok(concernService.updateConcernStatus(concernId, request));
    }
}