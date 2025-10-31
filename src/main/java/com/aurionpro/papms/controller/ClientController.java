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

    // == INVOICE ENDPOINTS ==
    @PostMapping("/invoices")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto request) {
        log.info("Request to create a new invoice #{} for client ID: {}", request.getInvoiceNumber(), request.getClientId());
        InvoiceResponseDto response = clientService.createInvoice(request);
        log.info("Successfully created invoice with ID: {}", response.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<List<InvoiceResponseDto>> getAllInvoices() {
        log.info("Request to get all invoices for the current user's organization/client profile.");
        return ResponseEntity.ok(clientService.getAllInvoicesForCurrentOrg());
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Integer invoiceId) {
        log.info("Request to get invoice by ID: {}", invoiceId);
        return ResponseEntity.ok(clientService.getInvoiceById(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> payInvoice(@PathVariable Integer invoiceId) {
        log.info("Request by client to pay invoice ID: {}", invoiceId);
        String result = clientService.processInvoicePayment(invoiceId);
        log.info("Payment for invoice ID {} processed successfully.", invoiceId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices/{invoiceId}/download")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Integer invoiceId) {
        log.info("Request to download PDF for invoice ID: {}", invoiceId);
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoiceId);
        log.info("Successfully generated PDF for invoice ID: {}", invoiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/organizations/{organizationId}/clients")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Page<ClientResponseDto>> getClientsForOrganization(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable) {
        log.info("Request to get clients for organization ID {} with pagination: {}", organizationId, pageable);
        Page<ClientResponseDto> clientsPage = clientService.getAllClientsForOrganization(organizationId, pageable);
        log.info("Returning {} clients on page {} for org ID {}", clientsPage.getNumberOfElements(), pageable.getPageNumber(), organizationId);
        return ResponseEntity.ok(clientsPage);
    }
}