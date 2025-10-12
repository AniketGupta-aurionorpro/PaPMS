package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.aurionpro.papms.service.PayslipPdfService;
import com.aurionpro.papms.service.PayrollExcelReportService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import java.io.IOException;
import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;
import com.aurionpro.papms.service.PayrollService;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/employees")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController {

    private final EmployeeService employeeService;
    private final PayslipPdfService payslipPdfService; // Inject new service
    private final PayrollExcelReportService payrollExcelReportService;
    private final PayrollService payrollService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> addEmployee(
            @PathVariable Integer organizationId,
            @Valid @RequestBody AddEmployeeRequest request) {
        log.info("Request to add a single employee with username {} to organization ID {}", request.getUsername(), organizationId);
        employeeService.addEmployee(organizationId, request);
        log.info("Successfully added employee {}", request.getUsername());
        return ResponseEntity.ok("Employee added successfully");
    }

    @PostMapping(path = "/bulk-upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Bulk upload employees via CSV")
    public ResponseEntity<BulkEmployeeUploadResponse> bulkUploadEmployees(
            @PathVariable Integer organizationId,
            @RequestParam("file") MultipartFile file) {
        log.info("Request for bulk employee upload for organization ID {}. File: {}", organizationId, file.getOriginalFilename());
        BulkEmployeeUploadResponse response = employeeService.bulkAddEmployees(organizationId, file);
        log.info("Bulk upload completed for organization ID {}. Success: {}, Failed: {}", organizationId, response.getSuccessfulImports(), response.getFailedImports());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<Page<EmployeeResponseDto>> getEmployeesByOrganization(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable)  { // <-- Spring automatically creates this from URL params
        log.info("Request to get employees for organization ID {} with pagination {}", organizationId, pageable);
        Page<EmployeeResponseDto> employeesPage = employeeService.getEmployeesByOrganization(organizationId, pageable);
        log.info("Returning {} employees for organization ID {}", employeesPage.getTotalElements(), organizationId);
        return ResponseEntity.ok(employeesPage);
    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(
            @PathVariable Long employeeId) {
        log.info("Request to get employee by ID: {}", employeeId);
        EmployeeResponseDto employee = employeeService.getEmployeeById( employeeId);
        return ResponseEntity.ok(employee);
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> deleteEmployee(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        log.info("Request to delete (deactivate) employee ID {} from organization ID {}", employeeId, organizationId);
        employeeService.deleteEmployee(organizationId, employeeId);
        log.info("Successfully deactivated employee ID {}", employeeId);
        return ResponseEntity.ok("Employee deleted successfully");
    }

    @PostMapping("/complete")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> addCompleteEmployee(
            @PathVariable Integer organizationId,
            @Valid @RequestBody CompleteEmployeeRequest request) {
        log.info("Request to add a complete employee with username {} to organization ID {}", request.getUsername(), organizationId);
        CompleteEmployeeResponse response = employeeService.addCompleteEmployee(organizationId, request);
        log.info("Successfully added complete employee with ID {}", response.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // New endpoint to get complete employee details
    @GetMapping("/complete")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<List<CompleteEmployeeResponse>> getCompleteEmployeesByOrganization(
            @PathVariable Integer organizationId) {
        log.info("Request to get all complete employee profiles for organization ID {}", organizationId);
        List<CompleteEmployeeResponse> employees = employeeService.getCompleteEmployeesByOrganization(organizationId);
        return ResponseEntity.ok(employees);
    }

    // New endpoint to get specific complete employee
    @GetMapping("/complete/{employeeId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> getCompleteEmployeeById(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        log.info("Request to get complete employee profile for ID {} in organization ID {}", employeeId, organizationId);
        CompleteEmployeeResponse employee = employeeService.getCompleteEmployeeById(organizationId, employeeId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeByUsername(@PathVariable String username) {
        log.info("Request to get employee by username: {}", username);
        return ResponseEntity.ok(employeeService.getEmployeeByUsername(username));
    }
    @PutMapping("/{employeeId}/profile")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeProfile(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("Request by employee ID {} to update their own profile", employeeId);
        CompleteEmployeeResponse response = employeeService.updateEmployeeProfile(employeeId, request);
        log.info("Employee profile for ID {} updated successfully.", employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/password")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long employeeId,
            @Valid @RequestBody ChangePasswordRequest request) {
        log.info("Request by employee ID {} to change their own password", employeeId);
        employeeService.changePassword(employeeId, request);
        log.info("Password changed successfully for employee ID {}", employeeId);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PutMapping("/{employeeId}/bank-account")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CompleteEmployeeResponse> updateBankAccount(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest.UpdateBankAccountRequest request) {
        log.info("Request by employee ID {} to update their own bank account", employeeId);
        CompleteEmployeeResponse response = employeeService.updateBankAccount(employeeId, request);
        log.info("Bank account for employee ID {} updated successfully.", employeeId);
        return ResponseEntity.ok(response);
    }

    // ===== ORGANIZATION ADMIN ENDPOINTS =====

    @PutMapping("/{employeeId}/details")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeDetails(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        log.info("Admin request to update details for employee ID {} in org ID {}", employeeId, organizationId);
        CompleteEmployeeResponse response = employeeService.updateEmployeeDetails(organizationId, employeeId, request);
        log.info("Details updated successfully for employee ID {}", employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/salary")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeSalary(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateSalaryRequest request) {
        log.info("Admin request to update salary for employee ID {} in org ID {}", employeeId, organizationId);
        CompleteEmployeeResponse response = employeeService.updateEmployeeSalary(organizationId, employeeId, request);
        log.info("Salary updated successfully for employee ID {}", employeeId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> toggleEmployeeStatus(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @RequestParam boolean active) {
        log.info("Admin request to set active status to {} for employee ID {} in org ID {}", active, employeeId, organizationId);
        CompleteEmployeeResponse response = employeeService.toggleEmployeeStatus(organizationId, employeeId, active);
        log.info("Status for employee ID {} set to {}", employeeId, active);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/bulk-upload-batch", consumes = "multipart/form-data") // New endpoint name
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Bulk upload employees via CSV using asynchronous Batch Processing")
    public ResponseEntity<String> bulkUploadEmployeesBatch(
            @PathVariable Integer organizationId,
            @RequestParam("file") MultipartFile file) {
        log.info("Request to launch asynchronous batch import job for organization ID {}", organizationId);
        try {
            String responseMessage = employeeService.launchCsvImportJob(organizationId, file);
            log.info("Batch job for organization ID {} launched successfully.", organizationId);
            return ResponseEntity.accepted().body(responseMessage); // Return 202 Accepted
        } catch (Exception e) {
            log.error("Failed to LAUNCH the CSV import job for organization ID {}", organizationId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start the CSV import job: " + e.getMessage());
        }
    }

    @GetMapping("/payslips/{paymentId}/download")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Download a salary slip as PDF", description = "Employees can download their own salary slips.")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Integer organizationId, @PathVariable Long paymentId) {
        log.info("Request to download payslip PDF for payment ID {}", paymentId);
        byte[] pdfBytes = payslipPdfService.generatePayslip(paymentId);
        log.info("Successfully generated payslip PDF for payment ID {}", paymentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "payslip-" + paymentId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/payrolls/report/excel")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Download a monthly payroll report as Excel")
    public ResponseEntity<byte[]> downloadPayrollReport(
            @PathVariable Integer organizationId,
            @RequestParam int year,
            @RequestParam int month) throws IOException {
        log.info("Request to download payroll Excel report for org ID {}, month/year: {}/{}", organizationId, month, year);
        byte[] excelBytes = payrollExcelReportService.generatePayrollReport(organizationId, year, month);
        log.info("Successfully generated Excel report for org ID {}", organizationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "payroll-report-" + month + "-" + year + ".xlsx");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
    @GetMapping("/payslips/{paymentId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE', 'ORG_ADMIN')")
    @Operation(summary = "Get detailed payslip data", description = "Provides a full salary breakdown for a specific payment. Employees can only access their own.")
    public ResponseEntity<PayrollPaymentResponse> getPayslipDetails(
            @PathVariable Integer organizationId,
            @PathVariable Long paymentId) {
        log.info("Request to get payslip details for payment ID {}", paymentId);
        PayrollPaymentResponse payslipData = payrollService.getPayrollPaymentDetails(paymentId);
        return ResponseEntity.ok(payslipData);
    }

    @GetMapping("/me/payslips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my paginated payslip history", description = "Returns a paginated list of all historical payslips for the logged-in employee.")
    public ResponseEntity<Page<MyPayslipHistoryDto>> getMyPayslipHistory(@ParameterObject Pageable pageable) {
        log.info("Request by current employee to get their payslip history with pagination {}", pageable);
        return ResponseEntity.ok(payrollService.getMyPayslipHistory(pageable));
    }

    @PostMapping(path = "/{employeeId}/profile-picture", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Upload or replace an employee's profile picture")
    public ResponseEntity<CompleteEmployeeResponse> uploadProfilePicture(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file) {
        log.info("Request to upload profile picture for employee ID {} in org ID {}. File: {}", employeeId, organizationId, file.getOriginalFilename());
        CompleteEmployeeResponse response = employeeService.uploadProfilePicture(organizationId, employeeId, file);
        log.info("Successfully uploaded profile picture for employee ID {}. New URL: {}", employeeId, response.getProfilePictureUrl());
        return ResponseEntity.ok(response);
    }
}