package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.ClientDepositRequestDto;
import com.aurionpro.papms.security.CustomUserDetails;
import com.aurionpro.papms.service.ClientDepositService;
import com.aurionpro.papms.service.client.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Client Deposit Request operations
 */
@RestController
@RequestMapping("/api/client-deposits")
@RequiredArgsConstructor
@Slf4j
public class ClientDepositController {

    private final ClientDepositService clientDepositService;
    private final ClientPortalService clientPortalService;

    /**
     * Create a new deposit request (CLIENT)
     * POST /api/client-deposits
     */
    @PostMapping
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientDepositRequestDto.Response> createDepositRequest(
            @Valid @RequestBody ClientDepositRequestDto.CreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Client {} creating deposit request for amount: {}",
                userDetails.getUsername(), request.getAmount());

        // Get client ID from user
        Long clientId = clientPortalService.getClientIdByUserId(userDetails.getId());

        ClientDepositRequestDto.Response response = clientDepositService.createDepositRequest(request, clientId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get my deposit requests (CLIENT)
     * GET /api/client-deposits/my-requests
     */
    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<List<ClientDepositRequestDto.Response>> getMyDepositRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long clientId = clientPortalService.getClientIdByUserId(userDetails.getId());
        List<ClientDepositRequestDto.Response> requests = clientDepositService.getClientDepositRequests(clientId);
        return ResponseEntity.ok(requests);
    }

    /**
     * Get all deposit requests for organization (ORG_ADMIN)
     * GET /api/client-deposits/organization
     */
    @GetMapping("/organization")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<List<ClientDepositRequestDto.Response>> getOrganizationDepositRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("ORG_ADMIN {} fetching all deposit requests", userDetails.getUsername());
        List<ClientDepositRequestDto.Response> requests = clientDepositService
                .getOrganizationDepositRequests(userDetails.getOrganizationId());
        return ResponseEntity.ok(requests);
    }

    /**
     * Get pending deposit requests for organization (ORG_ADMIN)
     * GET /api/client-deposits/organization/pending
     */
    @GetMapping("/organization/pending")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<List<ClientDepositRequestDto.Response>> getPendingDepositRequests(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("ORG_ADMIN {} fetching pending deposit requests", userDetails.getUsername());
        List<ClientDepositRequestDto.Response> requests = clientDepositService
                .getPendingDepositRequests(userDetails.getOrganizationId());
        return ResponseEntity.ok(requests);
    }

    /**
     * Get pending count for organization (ORG_ADMIN)
     * GET /api/client-deposits/organization/pending-count
     */
    @GetMapping("/organization/pending-count")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<Map<String, Long>> getPendingCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        long count = clientDepositService.getPendingCount(userDetails.getOrganizationId());
        return ResponseEntity.ok(Map.of("pendingCount", count));
    }

    /**
     * Approve a deposit request (ORG_ADMIN)
     * POST /api/client-deposits/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientDepositRequestDto.Response> approveDeposit(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("ORG_ADMIN {} approving deposit request {}", userDetails.getUsername(), id);
        ClientDepositRequestDto.Response response = clientDepositService.approveDeposit(id,
                userDetails.getId().intValue());
        return ResponseEntity.ok(response);
    }

    /**
     * Reject a deposit request (ORG_ADMIN)
     * POST /api/client-deposits/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientDepositRequestDto.Response> rejectDeposit(
            @PathVariable Long id,
            @RequestBody ClientDepositRequestDto.ProcessRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("ORG_ADMIN {} rejecting deposit request {}", userDetails.getUsername(), id);
        ClientDepositRequestDto.Response response = clientDepositService.rejectDeposit(id, request.getRejectionReason(),
                userDetails.getId().intValue());
        return ResponseEntity.ok(response);
    }

    /**
     * Get deposit request by ID
     * GET /api/client-deposits/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<ClientDepositRequestDto.Response> getDepositById(@PathVariable Long id) {
        ClientDepositRequestDto.Response response = clientDepositService.getDepositById(id);
        return ResponseEntity.ok(response);
    }
}
