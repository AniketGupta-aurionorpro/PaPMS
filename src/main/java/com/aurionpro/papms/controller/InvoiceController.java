package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.InvoicePaymentDto.PaymentRequest;
import com.aurionpro.papms.dto.InvoicePaymentDto.PaymentResponse;
import com.aurionpro.papms.dto.InvoiceRequestDto;
import com.aurionpro.papms.dto.InvoiceResponseDto;
import com.aurionpro.papms.security.CustomUserDetails;
import com.aurionpro.papms.service.InvoiceService;
import com.aurionpro.papms.service.client.ClientPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for Invoice Management
 * Handles invoice creation, listing, PDF generation, and email sending
 */
@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Slf4j
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final ClientPortalService clientPortalService;

    /**
     * Create a new invoice
     * POST /api/invoices
     */
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> createInvoice(
            @Valid @RequestBody InvoiceRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Creating invoice for organization: {}", userDetails.getOrganizationId());
        InvoiceResponseDto response = invoiceService.createInvoice(request, userDetails.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Create invoice and send email immediately
     * POST /api/invoices/create-and-send
     */
    @PostMapping("/create-and-send")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> createAndSendInvoice(
            @Valid @RequestBody InvoiceRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Creating and sending invoice for organization: {}", userDetails.getOrganizationId());
        InvoiceResponseDto response = invoiceService.createAndSendInvoice(request, userDetails.getOrganizationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all invoices for the organization
     * GET /api/invoices
     */
    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<List<InvoiceResponseDto>> getOrganizationInvoices(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Fetching invoices for organization: {}", userDetails.getOrganizationId());
        List<InvoiceResponseDto> invoices = invoiceService.getInvoicesByOrganization(userDetails.getOrganizationId());
        return ResponseEntity.ok(invoices);
    }

    /**
     * Get invoice by ID
     * GET /api/invoices/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<InvoiceResponseDto> getInvoiceById(@PathVariable Integer id) {
        log.info("Fetching invoice with ID: {}", id);
        InvoiceResponseDto response = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Download invoice PDF
     * GET /api/invoices/{id}/pdf
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<byte[]> downloadInvoicePdf(@PathVariable Integer id) {
        log.info("Generating PDF for invoice: {}", id);

        byte[] pdfBytes = invoiceService.generateInvoicePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "Invoice_" + id + ".pdf");
        headers.setContentLength(pdfBytes.length);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }

    /**
     * Send invoice email to client
     * POST /api/invoices/{id}/send-email
     */
    @PostMapping("/{id}/send-email")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<Map<String, String>> sendInvoiceEmail(@PathVariable Integer id) {
        log.info("Sending email for invoice: {}", id);
        invoiceService.sendInvoiceEmail(id);
        return ResponseEntity.ok(Map.of("message", "Invoice email sent successfully"));
    }

    /**
     * Mark invoice as paid
     * PUT /api/invoices/{id}/mark-paid
     */
    @PutMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> markAsPaid(@PathVariable Integer id) {
        log.info("Marking invoice {} as paid", id);
        InvoiceResponseDto response = invoiceService.markAsPaid(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Cancel an invoice
     * PUT /api/invoices/{id}/cancel
     */
    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<InvoiceResponseDto> cancelInvoice(@PathVariable Integer id) {
        log.info("Cancelling invoice: {}", id);
        InvoiceResponseDto response = invoiceService.cancelInvoice(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Get invoices for a specific client (for CLIENT role)
     * GET /api/invoices/client/{clientId}
     */
    @GetMapping("/client/{clientId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'CLIENT')")
    public ResponseEntity<List<InvoiceResponseDto>> getClientInvoices(@PathVariable Long clientId) {
        log.info("Fetching invoices for client: {}", clientId);
        List<InvoiceResponseDto> invoices = invoiceService.getInvoicesByClient(clientId);
        return ResponseEntity.ok(invoices);
    }

    /**
     * Pay an invoice (FULL or PARTIAL) from client wallet balance
     * POST /api/invoices/{id}/pay
     */
    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<PaymentResponse> payInvoice(
            @PathVariable Integer id,
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Client {} paying invoice {}", userDetails.getUsername(), id);

        // Get client ID from user
        Long clientId = clientPortalService.getClientIdByUserId(userDetails.getId());
        request.setInvoiceId(id);

        PaymentResponse response = invoiceService.payInvoice(request, clientId);
        return ResponseEntity.ok(response);
    }
}
