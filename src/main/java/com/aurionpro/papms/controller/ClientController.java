package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.service.ClientService;
import com.aurionpro.papms.service.InvoicePdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ClientController {

    private final ClientService clientService;
    private final InvoicePdfService invoicePdfService;

    // == CLIENT ENDPOINTS (for ORG_ADMIN) ==
    @PostMapping("/clients")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientResponseDto> createClient(@Valid @RequestBody ClientRequestDto request) {
        log.info("Request to create a new client with username: {}", request.getUsername());
        ClientResponseDto response = clientService.createClient(request);
        log.info("Successfully created client with ID: {}", response.getClientId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/clients")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<Page<ClientResponseDto>> getAllClients(@ParameterObject Pageable pageable) {
        log.info("Request to get all clients with pagination: {}", pageable);
        Page<ClientResponseDto> clientsPage = clientService.getAllClientsForCurrentOrg(pageable);
        log.info("Returning {} clients on page {}", clientsPage.getNumberOfElements(), pageable.getPageNumber());
        return ResponseEntity.ok(clientsPage);
    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientResponseDto> getClientById(@PathVariable Integer clientId) {
        log.info("Request to get client by ID: {}", clientId);
        return ResponseEntity.ok(clientService.getClientById(clientId));
    }

    // Invoice endpoints moved to InvoiceController to avoid duplicate mappings

    @GetMapping("/organizations/{organizationId}/clients")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Page<ClientResponseDto>> getClientsForOrganization(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable) {
        log.info("Request to get clients for organization ID {} with pagination: {}", organizationId, pageable);
        Page<ClientResponseDto> clientsPage = clientService.getAllClientsForOrganization(organizationId, pageable);
        log.info("Returning {} clients on page {} for org ID {}", clientsPage.getNumberOfElements(),
                pageable.getPageNumber(), organizationId);
        return ResponseEntity.ok(clientsPage);
    }
}