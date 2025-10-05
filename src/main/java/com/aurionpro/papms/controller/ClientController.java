package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.service.ClientService;
import com.aurionpro.papms.service.InvoicePdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
public class ClientController {

    private final ClientService clientService;
    private final InvoicePdfService invoicePdfService;

    // == CLIENT ENDPOINTS (for ORG_ADMIN) ==
    @PostMapping("/clients")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientResponseDto> createClient(@Valid @RequestBody ClientRequestDto request) {
        return new ResponseEntity<>(clientService.createClient(request), HttpStatus.CREATED);
    }


    @GetMapping("/clients")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<Page<ClientResponseDto>> getAllClients(@ParameterObject Pageable pageable) {
        Page<ClientResponseDto> clientsPage = clientService.getAllClientsForCurrentOrg(pageable);
        return ResponseEntity.ok(clientsPage);
    }
//    @GetMapping("/clients")
//    @PreAuthorize("hasRole('ORG_ADMIN')")
//    public ResponseEntity<List<ClientResponseDto>> getAllClients() {
//        return ResponseEntity.ok(clientService.getAllClientsForCurrentOrg());
//    }

    @GetMapping("/clients/{clientId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<ClientResponseDto> getClientById(@PathVariable Integer clientId) {
        return ResponseEntity.ok(clientService.getClientById(clientId));
    }

    // == INVOICE ENDPOINTS ==
    @PostMapping("/invoices")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(@Valid @RequestBody InvoiceRequestDto request) {
        return new ResponseEntity<>(clientService.createInvoice(request), HttpStatus.CREATED);
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<List<InvoiceResponseDto>> getAllInvoices() {
        // This endpoint can be enhanced to return invoices based on the user's role
        // For now, let's assume it's for the organization view.
        return ResponseEntity.ok(clientService.getAllInvoicesForCurrentOrg());
    }

    @GetMapping("/invoices/{invoiceId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Integer invoiceId) {
        return ResponseEntity.ok(clientService.getInvoiceById(invoiceId));
    }

    @PostMapping("/invoices/{invoiceId}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<String> payInvoice(@PathVariable Integer invoiceId) {
        String result = clientService.processInvoicePayment(invoiceId);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/invoices/{invoiceId}/download")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Integer invoiceId) {
        byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoiceId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "invoice-" + invoiceId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}