package com.aurionpro.papms.controller.client;

import com.aurionpro.papms.dto.ClientDto;
import com.aurionpro.papms.dto.OnboardClientRequest;
import com.aurionpro.papms.service.client.ClientPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.web.SortDefault;

/**
 * REST controller for Client Portal operations
 * Handles client onboarding and management
 */
@RestController
@RequestMapping("/api/clients/portal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Client Portal", description = "Client onboarding and management APIs")
public class ClientPortalController {

    private final ClientPortalService clientPortalService;

    @PostMapping("/onboard")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Onboard a new client", description = "Creates a client account with auto-generated credentials and sends welcome email")
    public ResponseEntity<ClientDto> onboardClient(@Valid @RequestBody OnboardClientRequest request) {
        log.info("Request to onboard client with email: {}", request.getContactEmail());
        ClientDto client = clientPortalService.onboardClient(request);
        log.info("Client {} onboarded successfully", client.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(client);
    }

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get all clients for organization with pagination")
    public ResponseEntity<Page<ClientDto>> getAllClients(
            @ParameterObject @SortDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        log.info("Request to get all clients for organization with pagination");
        Page<ClientDto> clients = clientPortalService.getAllClients(pageable);
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get client by ID")
    public ResponseEntity<ClientDto> getClientById(@PathVariable("id") Long clientId) {
        log.info("Request to get client with ID: {}", clientId);
        ClientDto client = clientPortalService.getClientById(clientId);
        return ResponseEntity.ok(client);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CLIENT')")
    @Operation(summary = "Get current client's profile", description = "For CLIENT role users only")
    public ResponseEntity<ClientDto> getMyProfile() {
        log.info("Request to get  client profile");
        ClientDto client = clientPortalService.getMyProfile();
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{id}/suspend")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Suspend a client account")
    public ResponseEntity<Void> suspendClient(@PathVariable("id") Long clientId) {
        log.info("Request to suspend client ID: {}", clientId);
        clientPortalService.suspendClient(clientId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Activate a suspended client account")
    public ResponseEntity<Void> activateClient(@PathVariable("id") Long clientId) {
        log.info("Request to activate client ID: {}", clientId);
        clientPortalService.activateClient(clientId);
        return ResponseEntity.noContent().build();
    }
}
