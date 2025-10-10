package com.aurionpro.papms.service;

import com.aurionpro.papms.entity.Invoice;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.InvoiceRepository;
import com.aurionpro.papms.utils.PdfStylingHelper; // NEW IMPORT
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor // Use Lombok constructor injection
public class InvoicePdfService {

    // MODIFIED: Inject InvoiceRepository directly to get the full entity
    private final InvoiceRepository invoiceRepository;

    private String valueOf(Object obj) {
        return Objects.toString(obj, "N/A");
    }

    @Transactional(readOnly = true)
    public byte[] generateInvoicePdf(Integer invoiceId) {
        // Fetch the full entity to get access to related objects like Organization
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(36, 36, 50, 36); // top, right, bottom, left

        try {
            // Use the styling helper for a professional header
            PdfStylingHelper.addLogoAndTitle(document, invoice.getOrganization(), "INVOICE");

            // --- Billed From/To and Invoice Details Section ---
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
            detailsTable.setBorder(Border.NO_BORDER);

            // Billed From (Left side)
            Cell fromCell = new Cell(1, 2).setBorder(Border.NO_BORDER).setPadding(10);
            fromCell.add(new Paragraph("BILLED FROM").setBold().setFontColor(PdfStylingHelper.PRIMARY_COLOR));
            fromCell.add(new Paragraph(valueOf(invoice.getOrganization().getCompanyName())).setBold());
            fromCell.add(new Paragraph(valueOf(invoice.getOrganization().getAddress())));
            fromCell.add(new Paragraph(valueOf(invoice.getOrganization().getContactEmail())));
            detailsTable.addCell(fromCell);

            // Invoice Details (Right side)
            Table nestedDetails = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            nestedDetails.addCell(PdfStylingHelper.createLabelCell("Invoice #"));
            nestedDetails.addCell(PdfStylingHelper.createValueCell(valueOf(invoice.getInvoiceNumber()), TextAlignment.RIGHT));
            nestedDetails.addCell(PdfStylingHelper.createLabelCell("Issue Date"));
            nestedDetails.addCell(PdfStylingHelper.createValueCell(invoice.getIssueDate().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.RIGHT));
            nestedDetails.addCell(PdfStylingHelper.createLabelCell("Due Date"));
            nestedDetails.addCell(PdfStylingHelper.createValueCell(invoice.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.RIGHT));
            detailsTable.addCell(new Cell(1, 2).add(nestedDetails).setBorder(Border.NO_BORDER));

            // Billed To
            Cell toCell = new Cell(1, 2).setBorder(Border.NO_BORDER).setPadding(10);
            toCell.add(new Paragraph("BILLED TO").setBold().setFontColor(PdfStylingHelper.PRIMARY_COLOR));
            toCell.add(new Paragraph(valueOf(invoice.getClient().getCompanyName())).setBold());
            toCell.add(new Paragraph("Attn: " + valueOf(invoice.getClient().getContactPerson())));
            toCell.add(new Paragraph(valueOf(invoice.getClient().getUser().getEmail())));
            detailsTable.addCell(toCell);

            document.add(detailsTable);
            document.add(new Paragraph("\n"));

            // --- Invoice Items Table ---
            Table itemTable = new Table(UnitValue.createPercentArray(new float[]{4, 1})).useAllAvailableWidth();
            itemTable.addHeaderCell(PdfStylingHelper.createHeaderCell("Description"));
            itemTable.addHeaderCell(PdfStylingHelper.createHeaderCell("Amount (INR)"));

            // Since there are no line items, we create a single descriptive row
            itemTable.addCell(new Cell().add(new Paragraph("Services Rendered / Products Sold")).setPadding(8));
            itemTable.addCell(new Cell().add(new Paragraph(invoice.getAmount().toPlainString())).setTextAlignment(TextAlignment.RIGHT).setPadding(8));
            document.add(itemTable);

            // --- Totals Section ---
            Table totalTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
            totalTable.setBorder(Border.NO_BORDER).setMarginLeft(300); // Push to the right
            totalTable.addCell(PdfStylingHelper.createLabelCell("Total"));
            totalTable.addCell(PdfStylingHelper.createValueCell("₹ " + invoice.getAmount().toPlainString(), TextAlignment.RIGHT).setBold().setFontSize(14));

            document.add(totalTable);

            // --- Status ---
            document.add(new Paragraph("Status: " + invoice.getStatus())
                    .setTextAlignment(TextAlignment.RIGHT).setBold().setFontColor(PdfStylingHelper.PRIMARY_COLOR));

            // Add footer at the end
            PdfStylingHelper.addFooter(document);

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        } finally {
            document.close();
        }

        return baos.toByteArray();
    }
}