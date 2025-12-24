package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.InvoicePaymentDto;
import com.aurionpro.papms.dto.InvoiceRequestDto;
import com.aurionpro.papms.dto.InvoiceResponseDto;

import java.util.List;

/**
 * Service interface for Invoice operations
 */
public interface InvoiceService {

    /**
     * Create a new invoice for a client
     * 
     * @param request        Invoice creation request
     * @param organizationId Organization ID from authenticated user
     * @return Created invoice response
     */
    InvoiceResponseDto createInvoice(InvoiceRequestDto request, Integer organizationId);

    /**
     * Get invoice by ID
     * 
     * @param invoiceId Invoice ID
     * @return Invoice response
     */
    InvoiceResponseDto getInvoiceById(Integer invoiceId);

    /**
     * Get all invoices for an organization
     * 
     * @param organizationId Organization ID
     * @return List of invoices
     */
    List<InvoiceResponseDto> getInvoicesByOrganization(Integer organizationId);

    /**
     * Get all invoices for a client
     * 
     * @param clientId Client ID
     * @return List of invoices
     */
    List<InvoiceResponseDto> getInvoicesByClient(Long clientId);

    /**
     * Generate PDF for an invoice
     * 
     * @param invoiceId Invoice ID
     * @return PDF as byte array
     */
    byte[] generateInvoicePdf(Integer invoiceId);

    /**
     * Send invoice email with PDF attachment to client
     * 
     * @param invoiceId Invoice ID
     */
    void sendInvoiceEmail(Integer invoiceId);

    /**
     * Create invoice and immediately send email to client
     * 
     * @param request        Invoice creation request
     * @param organizationId Organization ID
     * @return Created invoice response
     */
    InvoiceResponseDto createAndSendInvoice(InvoiceRequestDto request, Integer organizationId);

    /**
     * Mark invoice as paid
     * 
     * @param invoiceId Invoice ID
     * @return Updated invoice response
     */
    InvoiceResponseDto markAsPaid(Integer invoiceId);

    /**
     * Cancel an invoice
     * 
     * @param invoiceId Invoice ID
     * @return Updated invoice response
     */
    InvoiceResponseDto cancelInvoice(Integer invoiceId);

    /**
     * Pay an invoice (FULL or PARTIAL) from client wallet balance
     * 
     * @param request  Payment request with mode and amount
     * @param clientId Client ID making the payment
     * @return Payment response with updated balance
     */
    InvoicePaymentDto.PaymentResponse payInvoice(InvoicePaymentDto.PaymentRequest request, Long clientId);
}
