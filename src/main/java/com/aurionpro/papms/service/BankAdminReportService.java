package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.ReportRequestDto;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.PayrollBatch;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BankAdminReportService {

    private final TransactionRepository transactionRepository;
    private final PayrollBatchRepository payrollBatchRepository;
    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final VendorRepository vendorRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Transactional(readOnly = true)
    public byte[] generateTransactionReport(ReportRequestDto request) throws IOException {
        Specification<Transaction> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("transactionDate"), request.getStartDate().atStartOfDay()));
            }
            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("transactionDate"), request.getEndDate().atTime(23, 59, 59)));
            }
            if (!"ALL".equals(request.getOrganizationId())) {
                predicates.add(cb.equal(root.get("organization").get("id"), Integer.parseInt(request.getOrganizationId())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<Transaction> data = transactionRepository.findAll(spec);
        String[] headers = {"ID", "Date", "Organization", "Type", "Amount", "Description", "Source", "Balance After"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = createSheetWithHeader(workbook, "Transaction Report", headers);
            int rowIdx = 1;
            for (Transaction tx : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tx.getId());
                row.createCell(1).setCellValue(tx.getTransactionDate().format(DATE_FORMATTER));
                row.createCell(2).setCellValue(tx.getOrganization().getCompanyName());
                row.createCell(3).setCellValue(tx.getTransactionType().name());
                row.createCell(4).setCellValue(tx.getAmount().doubleValue());
                row.createCell(5).setCellValue(tx.getDescription());
                row.createCell(6).setCellValue(tx.getSourceType().name());
                row.createCell(7).setCellValue(tx.getBalanceAfterTransaction().doubleValue());
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePayrollReport(ReportRequestDto request) throws IOException {
        Specification<PayrollBatch> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (request.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), request.getStartDate().atStartOfDay()));
            }
            if (request.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), request.getEndDate().atTime(23, 59, 59)));
            }
            if (!"ALL".equals(request.getOrganizationId())) {
                predicates.add(cb.equal(root.get("organization").get("id"), Integer.parseInt(request.getOrganizationId())));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<PayrollBatch> data = payrollBatchRepository.findAll(spec);
        String[] headers = {"ID", "Organization", "Period", "Total Amount", "Total Employees", "Status", "Submitted By", "Approved By", "Submission Date"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = createSheetWithHeader(workbook, "Payroll Report", headers);
            int rowIdx = 1;
            for (PayrollBatch batch : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(batch.getId());
                row.createCell(1).setCellValue(batch.getOrganization().getCompanyName());
                row.createCell(2).setCellValue(batch.getPayrollMonth() + "/" + batch.getPayrollYear());
                row.createCell(3).setCellValue(batch.getTotalAmount().doubleValue());
                row.createCell(4).setCellValue(batch.getTotalEmployees());
                row.createCell(5).setCellValue(batch.getStatus().name());
                row.createCell(6).setCellValue(batch.getSubmittedByUser() != null ? batch.getSubmittedByUser().getFullName() : "N/A");
                row.createCell(7).setCellValue(batch.getApprovedByUser() != null ? batch.getApprovedByUser().getFullName() : "N/A");
                row.createCell(8).setCellValue(batch.getCreatedAt().format(DATE_FORMATTER));
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateOrganizationReport() throws IOException {
        List<Organization> data = organizationRepository.findAll();
        String[] headers = {"ID", "Company Name", "Contact Email", "Status", "Account Number", "Balance", "Registration Date"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = createSheetWithHeader(workbook, "Organization Report", headers);
            int rowIdx = 1;
            for (Organization org : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(org.getId());
                row.createCell(1).setCellValue(org.getCompanyName());
                row.createCell(2).setCellValue(org.getContactEmail());
                row.createCell(3).setCellValue(org.getStatus().name());
                row.createCell(4).setCellValue(org.getBankAssignedAccountNumber());
                row.createCell(5).setCellValue(org.getInternalBalance().doubleValue());
                row.createCell(6).setCellValue(org.getCreatedAt().format(DATE_FORMATTER));
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateEmployeeReport(ReportRequestDto request) throws IOException {
        Specification<Employee> spec = (root, query, cb) -> {
            if ("ALL".equals(request.getOrganizationId())) {
                return cb.conjunction();
            }
            return cb.equal(root.get("organization").get("id"), Integer.parseInt(request.getOrganizationId()));
        };
        List<Employee> data = employeeRepository.findAll(spec);
        String[] headers = {"ID", "Full Name", "Email", "Organization", "Employee Code", "Department", "Job Title", "Status"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = createSheetWithHeader(workbook, "Employee Report", headers);
            int rowIdx = 1;
            for (Employee emp : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(emp.getId());
                row.createCell(1).setCellValue(emp.getUser().getFullName());
                row.createCell(2).setCellValue(emp.getUser().getEmail());
                row.createCell(3).setCellValue(emp.getOrganization().getCompanyName());
                row.createCell(4).setCellValue(emp.getEmployeeCode());
                row.createCell(5).setCellValue(emp.getDepartment());
                row.createCell(6).setCellValue(emp.getJobTitle());
                row.createCell(7).setCellValue(emp.getIsActive() ? "Active" : "Inactive");
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateVendorReport(ReportRequestDto request) throws IOException {
        Specification<Vendor> spec = (root, query, cb) -> {
            if ("ALL".equals(request.getOrganizationId())) {
                return cb.conjunction();
            }
            return cb.equal(root.get("organization").get("id"), Integer.parseInt(request.getOrganizationId()));
        };
        List<Vendor> data = vendorRepository.findAll(spec);
        String[] headers = {"ID", "Vendor Name", "Contact Email", "Organization", "Status"};
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = createSheetWithHeader(workbook, "Vendor Report", headers);
            int rowIdx = 1;
            for (Vendor vendor : data) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(vendor.getId());
                row.createCell(1).setCellValue(vendor.getVendorName());
                row.createCell(2).setCellValue(vendor.getContactEmail());
                row.createCell(3).setCellValue(vendor.getOrganization().getCompanyName());
                row.createCell(4).setCellValue(vendor.getIsActive() ? "Active" : "Inactive");
            }
            autoSizeColumns(sheet, headers.length);
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // Helper Methods for Excel Generation
    private Sheet createSheetWithHeader(Workbook workbook, String sheetName, String[] headers) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        headerStyle.setFont(font);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        return sheet;
    }

    private void autoSizeColumns(Sheet sheet, int numColumns) {
        for (int i = 0; i < numColumns; i++) {
            sheet.autoSizeColumn(i);
        }
    }
}