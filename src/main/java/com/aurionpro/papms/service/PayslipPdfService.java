package com.aurionpro.papms.service;

import com.aurionpro.papms.entity.PayrollPayment;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.PayrollPaymentRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
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

        // Security check: Employee can only download their own payslip
        if (!payment.getEmployee().getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You are not authorized to download this payslip.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(36, 36, 36, 36);

        // --- PDF Content ---
        addHeader(document, payment);
        addEmployeeDetails(document, payment);
        addSalaryDetails(document, payment);
        addFooter(document);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, PayrollPayment payment) {
        document.add(new Paragraph(payment.getPayrollBatch().getOrganization().getCompanyName())
                .setTextAlignment(TextAlignment.CENTER).setBold().setFontSize(18));
        document.add(new Paragraph("Salary Slip")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(14));
        String monthYear = String.format(Locale.US, "%tB %d",
                payment.getPayrollBatch().getPayrollMonth() - 1, payment.getPayrollBatch().getPayrollYear());
        document.add(new Paragraph("For the month of " + monthYear)
                .setTextAlignment(TextAlignment.CENTER).setFontSize(14));
        document.add(new Paragraph("\n"));
    }

    private void addEmployeeDetails(Document document, PayrollPayment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2})).useAllAvailableWidth();
        table.setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 1));

        table.addCell(createCell("Employee Name:", true));
        table.addCell(createCell(payment.getEmployee().getUser().getFullName(), false));
        table.addCell(createCell("Employee Code:", true));
        table.addCell(createCell(payment.getEmployee().getEmployeeCode(), false));

        table.addCell(createCell("Designation:", true));
        table.addCell(createCell(payment.getEmployee().getJobTitle(), false));
        table.addCell(createCell("Department:", true));
        table.addCell(createCell(payment.getEmployee().getDepartment(), false));

        table.addCell(createCell("Date of Joining:", true));
        table.addCell(createCell(payment.getEmployee().getDateOfJoining().format(DateTimeFormatter.ISO_LOCAL_DATE), false));
        table.addCell(createCell("Payment Date:", true));
        table.addCell(createCell(payment.getProcessedAt().format(DateTimeFormatter.ISO_LOCAL_DATE), false));

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addSalaryDetails(Document document, PayrollPayment payment) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{3, 1, 3, 1})).useAllAvailableWidth();

        table.addHeaderCell(createHeaderCell("Earnings"));
        table.addHeaderCell(createHeaderCell("Amount (INR)"));
        table.addHeaderCell(createHeaderCell("Deductions"));
        table.addHeaderCell(createHeaderCell("Amount (INR)"));

        BigDecimal totalEarnings = payment.getBasicSalary().add(payment.getHra()).add(payment.getDa()).add(payment.getOtherAllowances());
        BigDecimal totalDeductions = payment.getPfContribution();

        table.addCell(createCell("Basic Salary", false));
        table.addCell(createAmountCell(payment.getBasicSalary()));
        table.addCell(createCell("Provident Fund (PF)", false));
        table.addCell(createAmountCell(payment.getPfContribution()));

        table.addCell(createCell("House Rent Allowance (HRA)", false));
        table.addCell(createAmountCell(payment.getHra()));
        table.addCell(createCell("", false)); // Empty cell
        table.addCell(createCell("", false)); // Empty cell

        table.addCell(createCell("Dearness Allowance (DA)", false));
        table.addCell(createAmountCell(payment.getDa()));
        table.addCell(createCell("", false));
        table.addCell(createCell("", false));

        table.addCell(createCell("Other Allowances", false));
        table.addCell(createAmountCell(payment.getOtherAllowances()));
        table.addCell(createCell("", false));
        table.addCell(createCell("", false));

        // Totals
        table.addCell(createTotalCell("Total Earnings", true));
        table.addCell(createTotalAmountCell(totalEarnings));
        table.addCell(createTotalCell("Total Deductions", true));
        table.addCell(createTotalAmountCell(totalDeductions));

        document.add(table);
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("Net Salary: ₹ " + payment.getNetSalaryPaid().toPlainString())
                .setTextAlignment(TextAlignment.RIGHT).setBold().setFontSize(14));
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n\n\n"));
        document.add(new Paragraph("This is a computer-generated payslip and does not require a signature.")
                .setTextAlignment(TextAlignment.CENTER).setFontSize(8).setItalic());
    }

    private Cell createCell(String text, boolean isBold) {
        Cell cell = new Cell().add(new Paragraph(text)).setPadding(5).setBorder(null);
        if (isBold) cell.setBold();
        return cell;
    }

    private Cell createAmountCell(BigDecimal amount) {
        return createCell(amount.toPlainString(), false).setTextAlignment(TextAlignment.RIGHT);
    }

    private Cell createHeaderCell(String text) {
        return new Cell().add(new Paragraph(text))
                .setBold().setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER).setPadding(5);
    }

    private Cell createTotalCell(String text, boolean isBold) {
        Cell cell = createCell(text, isBold);
        cell.setBorderTop(new SolidBorder(ColorConstants.BLACK, 1));
        return cell;
    }

    private Cell createTotalAmountCell(BigDecimal amount) {
        Cell cell = createAmountCell(amount).setBold();
        cell.setBorderTop(new SolidBorder(ColorConstants.BLACK, 1));
        return cell;
    }

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }
}