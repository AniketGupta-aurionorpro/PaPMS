package com.aurionpro.papms.service;

import com.aurionpro.papms.entity.PayrollPayment;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.PayrollPaymentRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class PayslipPdfService {

    private final PayrollPaymentRepository payrollPaymentRepository;
    private final AppUserRepository userRepository;

    @Transactional(readOnly = true)
    public byte[] generatePayslip(Long paymentId) {
        User currentUser = getLoggedInUser();
        PayrollPayment payment = payrollPaymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new NotFoundException("Payslip data not found for payment ID: " + paymentId));

        if (!payment.getEmployee().getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not authorized to download this payslip.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(36, 36, 50, 36);

        try {
            // Use helper for header with logo
            LocalDate payrollDate = LocalDate.of(payment.getPayrollBatch().getPayrollYear(), payment.getPayrollBatch().getPayrollMonth(), 1);
            String monthYear = String.format(Locale.US, "%tB %d", payrollDate, payment.getPayrollBatch().getPayrollYear());
            PdfStylingHelper.addLogoAndTitle(document, payment.getPayrollBatch().getOrganization(), "PAYSLIP");
            document.add(new Paragraph("For the month of " + monthYear)
                    .setTextAlignment(TextAlignment.CENTER).setFontSize(12).setItalic());
            document.add(new Paragraph("\n"));


            // Use styled cells for employee details
            addEmployeeDetails(document, payment);
            addSalaryDetails(document, payment);
            addFooter(document);

            PdfStylingHelper.addFooter(document);

        } finally {
            document.close();
        }
        return baos.toByteArray();
    }

    private void addEmployeeDetails(Document document, PayrollPayment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2})).useAllAvailableWidth();
        table.setBorder(new SolidBorder(PdfStylingHelper.BORDER_COLOR, 1));

        table.addCell(PdfStylingHelper.createLabelCell("Employee Name"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getEmployee().getUser().getFullName(), TextAlignment.LEFT));
        table.addCell(PdfStylingHelper.createLabelCell("Employee Code"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getEmployee().getEmployeeCode(), TextAlignment.LEFT));

        table.addCell(PdfStylingHelper.createLabelCell("Designation"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getEmployee().getJobTitle(), TextAlignment.LEFT));
        table.addCell(PdfStylingHelper.createLabelCell("Department"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getEmployee().getDepartment(), TextAlignment.LEFT));

        table.addCell(PdfStylingHelper.createLabelCell("Date of Joining"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getEmployee().getDateOfJoining().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.LEFT));
        table.addCell(PdfStylingHelper.createLabelCell("Payment Date"));
        table.addCell(PdfStylingHelper.createValueCell(payment.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE), TextAlignment.LEFT));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSalaryDetails(Document document, PayrollPayment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 3, 1})).useAllAvailableWidth();

        table.addHeaderCell(PdfStylingHelper.createHeaderCell("Earnings"));
        table.addHeaderCell(PdfStylingHelper.createHeaderCell("Amount (INR)"));
        table.addHeaderCell(PdfStylingHelper.createHeaderCell("Deductions"));
        table.addHeaderCell(PdfStylingHelper.createHeaderCell("Amount (INR)"));

        BigDecimal totalEarnings = payment.getBasicSalary().add(payment.getHra()).add(payment.getDa()).add(payment.getOtherAllowances());
        BigDecimal totalDeductions = payment.getPfContribution();

        // Data rows
        table.addCell(createDataCell("Basic Salary", false));
        table.addCell(createAmountCell(payment.getBasicSalary()));
        table.addCell(createDataCell("Provident Fund (PF)", false));
        table.addCell(createAmountCell(payment.getPfContribution()));
        table.addCell(createDataCell("House Rent Allowance (HRA)", false));
        table.addCell(createAmountCell(payment.getHra()));
        table.addCell(createDataCell("", false)); // Empty cell
        table.addCell(createAmountCell(null)); // Empty cell
        table.addCell(createDataCell("Dearness Allowance (DA)", false));
        table.addCell(createAmountCell(payment.getDa()));
        table.addCell(createDataCell("", false));
        table.addCell(createAmountCell(null));
        table.addCell(createDataCell("Other Allowances", false));
        table.addCell(createAmountCell(payment.getOtherAllowances()));
        table.addCell(createDataCell("", false));
        table.addCell(createAmountCell(null));

        // Totals row
        table.addCell(createTotalCell("Total Earnings"));
        table.addCell(createTotalAmountCell(totalEarnings));
        table.addCell(createTotalCell("Total Deductions"));
        table.addCell(createTotalAmountCell(totalDeductions));

        document.add(table);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Net Salary Payable: ₹ " + payment.getNetSalaryPaid().toPlainString())
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(14));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n\n\n"));
        document.add(new Paragraph("This is a computer-generated payslip and does not require a signature.")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(8).setItalic());
    }

    // Helper methods for salary table cells
    private Cell createDataCell(String text, boolean isBold) {
        Cell cell = new Cell().add(new Paragraph(text)).setPadding(5).setBorder(Border.NO_BORDER);
        if (isBold) cell.setBold();
        return cell;
    }
    private Cell createAmountCell(BigDecimal amount) {
        String text = (amount == null) ? "" : amount.toPlainString();
        return createDataCell(text, false).setTextAlignment(TextAlignment.RIGHT);
    }
    private Cell createTotalCell(String text) {
        Cell cell = createDataCell(text, true);
        cell.setBorderTop(new SolidBorder(PdfStylingHelper.BORDER_COLOR, 1));
        return cell;
    }
    private Cell createTotalAmountCell(BigDecimal amount) {
        Cell cell = createAmountCell(amount).setBold();
        cell.setBorderTop(new SolidBorder(PdfStylingHelper.BORDER_COLOR, 1));
        return cell;
    }

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }
}