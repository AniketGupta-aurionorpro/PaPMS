package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.InvoiceStatus;
import com.aurionpro.papms.dto.InvoicePaymentDto;
import com.aurionpro.papms.dto.InvoiceRequestDto;
import com.aurionpro.papms.dto.InvoiceResponseDto;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.Invoice;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.InvoiceMapper;
import com.aurionpro.papms.repository.ClientRepository;
import com.aurionpro.papms.repository.InvoiceRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of InvoiceService
 * Handles invoice creation, PDF generation, and email sending
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final InvoicePdfService invoicePdfService;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.display-name:PaPMS Support}")
    private String displayName;

    @Override
    public InvoiceResponseDto createInvoice(InvoiceRequestDto request, Integer organizationId) {
        log.info("Creating invoice for organization {} and client {}", organizationId, request.getClientId());

        // Validate organization
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        // Validate client belongs to organization
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + request.getClientId()));

        if (!client.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Client does not belong to this organization");
        }

        // Check for duplicate invoice number
        if (invoiceRepository.findByInvoiceNumberAndOrganizationId(request.getInvoiceNumber(), organizationId)
                .isPresent()) {
            throw new IllegalArgumentException("Invoice number already exists: " + request.getInvoiceNumber());
        }

        // Validate dates
        if (request.getDueDate().isBefore(request.getIssueDate())) {
            throw new IllegalArgumentException("Due date cannot be before the issue date");
        }

        // Create invoice
        Invoice invoice = InvoiceMapper.toEntity(request, organization, client);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice created successfully with ID: {}", invoice.getId());
        return InvoiceMapper.toDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));
        return InvoiceMapper.toDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoicesByOrganization(Integer organizationId) {
        return invoiceRepository.findByOrganizationId(organizationId)
                .stream()
                .map(InvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getInvoicesByClient(Long clientId) {
        return invoiceRepository.findByClientId(clientId.intValue())
                .stream()
                .map(InvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Integer invoiceId) {
        // Verify invoice exists
        invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        return invoicePdfService.generateInvoicePdf(invoiceId);
    }

    @Override
    @Async
    public void sendInvoiceEmail(Integer invoiceId) {
        log.info("Sending invoice email for invoice ID: {}", invoiceId);

        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        try {
            // Generate PDF
            byte[] pdfBytes = invoicePdfService.generateInvoicePdf(invoiceId);

            // Get client email
            String clientEmail = invoice.getClient().getContactEmail();
            String clientName = invoice.getClient().getClientName();
            String orgName = invoice.getOrganization().getCompanyName();

            // Create email
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setFrom(senderEmail, displayName);
            helper.setTo(clientEmail);
            helper.setSubject("Invoice #" + invoice.getInvoiceNumber() + " from " + orgName);

            // Build HTML email body
            String emailBody = buildInvoiceEmailBody(invoice, clientName, orgName);
            helper.setText(emailBody, true);

            // Attach PDF
            String filename = "Invoice_" + invoice.getInvoiceNumber() + ".pdf";
            helper.addAttachment(filename, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            log.info("Invoice email sent successfully to: {}", clientEmail);

        } catch (MessagingException e) {
            log.error("Failed to send invoice email for invoice {}: {}", invoiceId, e.getMessage());
            throw new RuntimeException("Failed to send invoice email", e);
        } catch (Exception e) {
            log.error("Error sending invoice email: {}", e.getMessage());
            throw new RuntimeException("Error sending invoice email", e);
        }
    }

    @Override
    public InvoiceResponseDto createAndSendInvoice(InvoiceRequestDto request, Integer organizationId) {
        InvoiceResponseDto response = createInvoice(request, organizationId);
        sendInvoiceEmail(response.getId());
        return response;
    }

    @Override
    public InvoiceResponseDto markAsPaid(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot mark cancelled invoice as paid");
        }

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice {} marked as paid", invoiceId);
        return InvoiceMapper.toDto(invoice);
    }

    @Override
    public InvoiceResponseDto cancelInvoice(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot cancel a paid invoice");
        }

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice = invoiceRepository.save(invoice);

        log.info("Invoice {} cancelled", invoiceId);
        return InvoiceMapper.toDto(invoice);
    }

    /**
     * Build HTML email body for invoice
     */
    private String buildInvoiceEmailBody(Invoice invoice, String clientName, String orgName) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        return """
                <html>
                <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                    <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                        <h2 style="color: #2563eb;">Invoice from %s</h2>

                        <p>Dear %s,</p>

                        <p>Please find attached the invoice for your reference.</p>

                        <div style="background: #f8fafc; padding: 20px; border-radius: 8px; margin: 20px 0;">
                            <table style="width: 100%%; border-collapse: collapse;">
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Invoice Number:</td>
                                    <td style="padding: 8px 0; font-weight: bold;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Amount:</td>
                                    <td style="padding: 8px 0; font-weight: bold; color: #16a34a;">₹%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Issue Date:</td>
                                    <td style="padding: 8px 0;">%s</td>
                                </tr>
                                <tr>
                                    <td style="padding: 8px 0; color: #64748b;">Due Date:</td>
                                    <td style="padding: 8px 0; font-weight: bold; color: #dc2626;">%s</td>
                                </tr>
                            </table>
                        </div>

                        <p>Please ensure payment is made by the due date to avoid any late fees.</p>

                        <p>If you have any questions, please don't hesitate to contact us.</p>

                        <p style="margin-top: 30px;">
                            Best regards,<br>
                            <strong>%s</strong>
                        </p>

                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #e2e8f0;">
                        <p style="font-size: 12px; color: #94a3b8;">
                            This is an automated email from PaPMS. Please do not reply directly to this email.
                        </p>
                    </div>
                </body>
                </html>
                """.formatted(
                orgName,
                clientName,
                invoice.getInvoiceNumber(),
                invoice.getAmount().toPlainString(),
                invoice.getIssueDate().format(dateFormatter),
                invoice.getDueDate().format(dateFormatter),
                orgName);
    }

    @Override
    public InvoicePaymentDto.PaymentResponse payInvoice(InvoicePaymentDto.PaymentRequest request, Long clientId) {
        log.info("Processing invoice payment for invoice {} by client {}, mode: {}",
                request.getInvoiceId(), clientId, request.getPaymentMode());

        // Find invoice
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + request.getInvoiceId()));

        // Find client
        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        // Verify invoice belongs to this client
        if (!invoice.getClient().getId().equals(clientId)) {
            throw new IllegalArgumentException("Invoice does not belong to this client");
        }

        // Validate invoice status
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Invoice is already fully paid");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new IllegalStateException("Cannot pay a cancelled invoice");
        }

        BigDecimal dueAmount = invoice.getDueAmount();
        BigDecimal paymentAmount;

        // Determine payment amount based on mode
        String mode = request.getPaymentMode().toUpperCase();
        if ("FULL".equals(mode)) {
            paymentAmount = dueAmount;
        } else if ("PARTIAL".equals(mode)) {
            if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount is required for partial payment");
            }
            if (request.getAmount().compareTo(dueAmount) > 0) {
                throw new IllegalArgumentException("Payment amount cannot exceed due amount");
            }
            paymentAmount = request.getAmount();
        } else {
            throw new IllegalArgumentException("Invalid payment mode. Use FULL or PARTIAL");
        }

        // Check client balance
        if (client.getBalance().compareTo(paymentAmount) < 0) {
            throw new IllegalStateException("Insufficient wallet balance. Available: " +
                    client.getBalance() + ", Required: " + paymentAmount);
        }

        // Deduct from client balance
        BigDecimal newClientBalance = client.getBalance().subtract(paymentAmount);
        client.setBalance(newClientBalance);
        clientRepository.save(client);
        log.info("Client {} balance reduced by {}. New balance: {}", clientId, paymentAmount, newClientBalance);

        // Update invoice
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(paymentAmount);
        invoice.setPaidAmount(newPaidAmount);

        // Check if fully paid
        if (invoice.isFullyPaid()) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(LocalDateTime.now());
            log.info("Invoice {} fully paid", invoice.getId());
        }

        invoice = invoiceRepository.save(invoice);

        // Build response
        String message = invoice.isFullyPaid()
                ? "Invoice fully paid successfully"
                : "Partial payment of ₹" + paymentAmount.toPlainString() + " recorded successfully";

        return InvoicePaymentDto.PaymentResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .totalAmount(invoice.getAmount())
                .paidAmount(invoice.getPaidAmount())
                .remainingAmount(invoice.getDueAmount())
                .status(invoice.getStatus().name())
                .clientBalanceAfter(newClientBalance)
                .message(message)
                .build();
    }
}
