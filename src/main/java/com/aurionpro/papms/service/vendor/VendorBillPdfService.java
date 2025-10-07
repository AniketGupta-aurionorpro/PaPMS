package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

@Service
public class VendorBillPdfService {

    private final BillService billService;

    public VendorBillPdfService(BillService billService) {
        this.billService = billService;
    }

    // A helper method to handle null values gracefully
    private String valueOf(Object obj) {
        return Objects.toString(obj, "N/A");
    }

    public byte[] generateVendorBillPdf(Long billId) {
        VendorBillDto bill = billService.getBillById(billId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // Title
            document.add(new Paragraph("Vendor Bill / Payment Advice")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBold()
                    .setFontSize(20));

            // Bill Details
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Bill Number: " + valueOf(bill.getBillNumber())));
            document.add(new Paragraph("Payment Date: " + valueOf(bill.getBillDate())));

            // Paid From and To
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Paid From:").setBold());
            document.add(new Paragraph(valueOf(bill.getOrganizationName())));

            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Paid To:").setBold());
            document.add(new Paragraph(valueOf(bill.getVendorName())));

            // Amount
            document.add(new Paragraph("\n\n"));
            document.add(new Paragraph("Amount Paid: " + valueOf(bill.getAmount()))
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(16));

            // Status
            document.add(new Paragraph("Status: " + valueOf(bill.getStatus()))
                    .setTextAlignment(TextAlignment.RIGHT));

        } catch (Exception e) {
            // It's good practice to log the actual error for debugging
            // log.error("Failed to generate PDF for bill ID {}: {}", billId, e.getMessage());
            throw new RuntimeException("Error generating PDF for vendor bill", e);
        }

        return baos.toByteArray();
    }
}