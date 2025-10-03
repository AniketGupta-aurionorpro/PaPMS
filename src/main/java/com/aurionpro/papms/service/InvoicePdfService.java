package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.InvoiceResponseDto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class InvoicePdfService {

    private final ClientService clientService;

    public InvoicePdfService(ClientService clientService) {
        this.clientService = clientService;
    }

    public byte[] generateInvoicePdf(Integer invoiceId) {
        InvoiceResponseDto invoice = clientService.getInvoiceById(invoiceId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        try {
            // Title
            document.add(new Paragraph("Invoice")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Invoice Details
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber()));
            document.add(new Paragraph("Issue Date: " + invoice.getIssueDate()));
            document.add(new Paragraph("Due Date: " + invoice.getDueDate()));

            // Billed To and From
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Billed From:").setBold());
            document.add(new Paragraph(invoice.getOrganizationName()));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Billed To:").setBold());
            document.add(new Paragraph(invoice.getClientCompanyName()));
            document.add(new Paragraph("Attn: " + invoice.getClientContactPerson()));

            // Amount
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Total Amount Due: " + invoice.getAmount())
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(16));

            // Status
            document.add(new Paragraph("Status: " + invoice.getStatus())
                    .setTextAlignment(TextAlignment.RIGHT));

            document.close();
        } catch (Exception e) {
            // Handle exceptions appropriately
            throw new RuntimeException("Error generating PDF", e);
        }

        return baos.toByteArray();
    }
}