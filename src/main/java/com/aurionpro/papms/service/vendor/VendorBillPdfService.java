package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.VendorBillRepository;
import com.aurionpro.papms.utils.PdfStylingHelper; // NEW IMPORT
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Service
@RequiredArgsConstructor // Use Lombok
public class VendorBillPdfService {

    // MODIFIED: Inject repository to fetch full entity
    private final VendorBillRepository vendorBillRepository;

    private String valueOf(Object obj) {
        return Objects.toString(obj, "N/A");
    }

    @Transactional(readOnly = true)
    public byte[] generateVendorBillPdf(Long billId) {
        VendorBill bill = vendorBillRepository.findById(billId)
                .orElseThrow(() -> new NotFoundException("Vendor Bill not found with ID: " + billId));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4)) {

            document.setMargins(36, 36, 50, 36);

            // Determine if bill is paid or pending
            boolean isPaid = bill.isFullyPaid() || bill.getVendorPayment() != null;
            String documentTitle = isPaid ? "PAYMENT ADVICE" : "VENDOR INVOICE";
            String amountColumnHeader = isPaid ? "Amount Paid (INR)" : "Amount Due (INR)";
            String fromLabel = isPaid ? "PAID FROM" : "BILL TO";
            String toLabel = isPaid ? "PAID TO" : "FROM VENDOR";
            String totalLabel = isPaid ? "Total Paid" : "Total Due";

            PdfStylingHelper.addLogoAndTitle(document, bill.getOrganization(), documentTitle);

            // --- From/To and Bill Details Section ---
            Table detailsTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1, 1, 1 }))
                    .useAllAvailableWidth();
            detailsTable.setBorder(Border.NO_BORDER);

            // From/Bill To
            Cell fromCell = new Cell(1, 2).setBorder(Border.NO_BORDER).setPadding(10);
            fromCell.add(new Paragraph(fromLabel).setBold().setFontColor(PdfStylingHelper.PRIMARY_COLOR));
            fromCell.add(new Paragraph(valueOf(bill.getOrganization().getCompanyName())).setBold());
            fromCell.add(new Paragraph(valueOf(bill.getOrganization().getAddress())));
            detailsTable.addCell(fromCell);

            // Bill Details
            Table nestedDetails = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).useAllAvailableWidth();
            nestedDetails.addCell(PdfStylingHelper.createLabelCell("Bill #"));
            nestedDetails.addCell(PdfStylingHelper.createValueCell(valueOf(bill.getBillNumber()), TextAlignment.RIGHT));
            nestedDetails.addCell(PdfStylingHelper.createLabelCell(isPaid ? "Payment Date" : "Bill Date"));
            nestedDetails.addCell(PdfStylingHelper
                    .createValueCell(bill.getBillDate().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.RIGHT));
            nestedDetails.addCell(PdfStylingHelper.createLabelCell("Status"));
            nestedDetails.addCell(PdfStylingHelper.createValueCell(bill.getStatus().name(), TextAlignment.RIGHT));
            if (!isPaid && bill.getDueDate() != null) {
                nestedDetails.addCell(PdfStylingHelper.createLabelCell("Due Date"));
                nestedDetails.addCell(PdfStylingHelper.createValueCell(
                        bill.getDueDate().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.RIGHT));
            }
            detailsTable.addCell(new Cell(1, 2).add(nestedDetails).setBorder(Border.NO_BORDER));

            // To/From Vendor
            Cell toCell = new Cell(1, 2).setBorder(Border.NO_BORDER).setPadding(10);
            toCell.add(new Paragraph(toLabel).setBold().setFontColor(PdfStylingHelper.PRIMARY_COLOR));
            toCell.add(new Paragraph(valueOf(bill.getVendor().getVendorName())).setBold());
            toCell.add(new Paragraph(valueOf(bill.getVendor().getAddress())));
            toCell.add(new Paragraph(valueOf(bill.getVendor().getContactEmail())));
            detailsTable.addCell(toCell);

            document.add(detailsTable);
            document.add(new Paragraph("\n"));

            // --- Items Table ---
            Table itemTable = new Table(UnitValue.createPercentArray(new float[] { 4, 1 })).useAllAvailableWidth();
            itemTable.addHeaderCell(PdfStylingHelper.createHeaderCell("Description"));
            itemTable.addHeaderCell(PdfStylingHelper.createHeaderCell(amountColumnHeader));

            // Handle case where bill hasn't been paid yet (vendorPayment is null)
            String description;
            if (bill.getVendorPayment() != null && bill.getVendorPayment().getDescription() != null) {
                description = bill.getVendorPayment().getDescription();
            } else if (bill.getDescription() != null) {
                description = bill.getDescription();
            } else {
                description = "Payment against services/goods";
            }
            itemTable.addCell(new Cell().add(new Paragraph(description)).setPadding(8));

            // Show appropriate amount based on status
            String displayAmount = isPaid ? bill.getPaidAmount().toPlainString() : bill.getDueAmount().toPlainString();
            itemTable.addCell(new Cell().add(new Paragraph(displayAmount))
                    .setTextAlignment(TextAlignment.RIGHT).setPadding(8));
            document.add(itemTable);

            // --- Totals Section ---
            Table totalTable = new Table(UnitValue.createPercentArray(new float[] { 1, 1 })).useAllAvailableWidth();
            totalTable.setBorder(Border.NO_BORDER).setMarginLeft(300);

            // Show bill amount breakdown
            totalTable.addCell(PdfStylingHelper.createLabelCell("Bill Amount"));
            totalTable.addCell(
                    PdfStylingHelper.createValueCell("₹ " + bill.getAmount().toPlainString(), TextAlignment.RIGHT));

            if (bill.getPaidAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                totalTable.addCell(PdfStylingHelper.createLabelCell("Paid Amount"));
                totalTable.addCell(PdfStylingHelper.createValueCell("₹ " + bill.getPaidAmount().toPlainString(),
                        TextAlignment.RIGHT));
            }

            totalTable.addCell(PdfStylingHelper.createLabelCell(totalLabel));
            totalTable.addCell(
                    PdfStylingHelper
                            .createValueCell("₹ " + (isPaid ? bill.getPaidAmount().toPlainString()
                                    : bill.getDueAmount().toPlainString()), TextAlignment.RIGHT)
                            .setBold().setFontSize(14));
            document.add(totalTable);

            PdfStylingHelper.addFooter(document);

        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF for vendor bill", e);
        }

        return baos.toByteArray();
    }
}