package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.ReportRequestDto;
import com.aurionpro.papms.service.BankAdminReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/bank-admin/reports")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bank Admin Reports", description = "APIs for Bank Administrators to generate system-wide reports")
public class BankAdminReportController {

    private final BankAdminReportService reportService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Operation(summary = "Generate and download a system report as an Excel file")
    public ResponseEntity<byte[]> generateReport(@RequestBody ReportRequestDto request) throws IOException {
        log.info("Request received to generate report of type: {}", request.getReportType());

        byte[] excelBytes;
        String reportName = request.getReportType().toLowerCase();

        switch (request.getReportType()) {
            case "TRANSACTION_REPORT":
                excelBytes = reportService.generateTransactionReport(request);
                break;
            case "PAYROLL_REPORT":
                excelBytes = reportService.generatePayrollReport(request);
                break;
            case "ORGANIZATION_REPORT":
                excelBytes = reportService.generateOrganizationReport();
                break;
            case "EMPLOYEE_REPORT":
                excelBytes = reportService.generateEmployeeReport(request);
                break;
            case "VENDOR_REPORT":
                excelBytes = reportService.generateVendorReport(request);
                break;
            default:
                log.warn("Invalid report type requested: {}", request.getReportType());
                return ResponseEntity.badRequest().build();
        }

        String filename = String.format("%s_%s.xlsx", reportName, LocalDate.now().format(DateTimeFormatter.ISO_DATE));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", filename);

        log.info("Successfully generated report '{}'. Sending file '{}' to client.", request.getReportType(), filename);
        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}