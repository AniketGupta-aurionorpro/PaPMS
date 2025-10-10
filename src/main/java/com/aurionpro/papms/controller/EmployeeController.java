package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        employeeService.addEmployee(organizationId, request);
        return ResponseEntity.ok("Employee added successfully");
    }

    @PostMapping(path = "/bulk-upload", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Bulk upload employees via CSV")

    public ResponseEntity<BulkEmployeeUploadResponse> bulkUploadEmployees(
            @PathVariable Integer organizationId,
            @RequestParam("file") MultipartFile file) {
        BulkEmployeeUploadResponse response = employeeService.bulkAddEmployees(organizationId, file);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<Page<EmployeeResponseDto>> getEmployeesByOrganization(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable)  { // <-- Spring automatically creates this from URL params
        Page<EmployeeResponseDto> employeesPage = employeeService.getEmployeesByOrganization(organizationId, pageable);
        return ResponseEntity.ok(employeesPage);
    }

//    @GetMapping
//    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
//    public ResponseEntity<List<EmployeeResponseDto>> getEmployeesByOrganization(
//            @PathVariable Integer organizationId) {
//        List<EmployeeResponseDto> employees = employeeService.getEmployeesByOrganization(organizationId);
//        return ResponseEntity.ok(employees);
//    }

    @GetMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<EmployeeResponseDto> getEmployeeById(
//            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        EmployeeResponseDto employee = employeeService.getEmployeeById( employeeId);
        return ResponseEntity.ok(employee);
    }

    @DeleteMapping("/{employeeId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> deleteEmployee(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        employeeService.deleteEmployee(organizationId, employeeId);
        return ResponseEntity.ok("Employee deleted successfully");
    }

//    @GetMapping("/{id}")
//    public ResponseEntity<EmployeeResponseDto> getEmployeeById(@PathVariable Long id) {
//        return ResponseEntity.ok(employeeService.getEmployeeById(id));
//    }


    @PostMapping("/complete")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> addCompleteEmployee(
            @PathVariable Integer organizationId,
            @Valid @RequestBody CompleteEmployeeRequest request) {
        CompleteEmployeeResponse response = employeeService.addCompleteEmployee(organizationId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // New endpoint to get complete employee details
    @GetMapping("/complete")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<List<CompleteEmployeeResponse>> getCompleteEmployeesByOrganization(
            @PathVariable Integer organizationId) {
        List<CompleteEmployeeResponse> employees = employeeService.getCompleteEmployeesByOrganization(organizationId);
        return ResponseEntity.ok(employees);
    }

    // New endpoint to get specific complete employee
    @GetMapping("/complete/{employeeId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> getCompleteEmployeeById(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        CompleteEmployeeResponse employee = employeeService.getCompleteEmployeeById(organizationId, employeeId);
        return ResponseEntity.ok(employee);
    }

    @GetMapping("/username/{username}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeByUsername(@PathVariable String username) {
        return ResponseEntity.ok(employeeService.getEmployeeByUsername(username));
    }
    @PutMapping("/{employeeId}/profile")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeProfile(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        CompleteEmployeeResponse response = employeeService.updateEmployeeProfile(employeeId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/password")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<String> changePassword(
            @PathVariable Long employeeId,
            @Valid @RequestBody ChangePasswordRequest request) {
        employeeService.changePassword(employeeId, request);
        return ResponseEntity.ok("Password changed successfully");
    }

    @PutMapping("/{employeeId}/bank-account")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<CompleteEmployeeResponse> updateBankAccount(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest.UpdateBankAccountRequest request) {
        CompleteEmployeeResponse response = employeeService.updateBankAccount(employeeId, request);
        return ResponseEntity.ok(response);
    }

    // ===== ORGANIZATION ADMIN ENDPOINTS =====

    @PutMapping("/{employeeId}/details")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeDetails(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        CompleteEmployeeResponse response = employeeService.updateEmployeeDetails(organizationId, employeeId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/salary")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> updateEmployeeSalary(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateSalaryRequest request) {
        CompleteEmployeeResponse response = employeeService.updateEmployeeSalary(organizationId, employeeId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{employeeId}/status")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<CompleteEmployeeResponse> toggleEmployeeStatus(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @RequestParam boolean active) {
        CompleteEmployeeResponse response = employeeService.toggleEmployeeStatus(organizationId, employeeId, active);
        return ResponseEntity.ok(response);
    }

    @PostMapping(path = "/bulk-upload-batch", consumes = "multipart/form-data") // New endpoint name
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Bulk upload employees via CSV using asynchronous Batch Processing")
    public ResponseEntity<String> bulkUploadEmployeesBatch(
            @PathVariable Integer organizationId,
            @RequestParam("file") MultipartFile file) {
        try {
            String responseMessage = employeeService.launchCsvImportJob(organizationId, file);
            return ResponseEntity.accepted().body(responseMessage); // Return 202 Accepted
        } catch (Exception e) {
            // This catches exceptions during job LAUNCH, not during job EXECUTION
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to start the CSV import job: " + e.getMessage());
        }
    }

    @GetMapping("/payslips/{paymentId}/download")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Download a salary slip as PDF", description = "Employees can download their own salary slips.")
    public ResponseEntity<byte[]> downloadPayslip(@PathVariable Integer organizationId, @PathVariable Long paymentId) {
        byte[] pdfBytes = payslipPdfService.generatePayslip(paymentId);

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

        byte[] excelBytes = payrollExcelReportService.generatePayrollReport(organizationId, year, month);

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
        PayrollPaymentResponse payslipData = payrollService.getPayrollPaymentDetails(paymentId);
        return ResponseEntity.ok(payslipData);
    }

    @GetMapping("/me/payslips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get my paginated payslip history", description = "Returns a paginated list of all historical payslips for the logged-in employee.")
    public ResponseEntity<Page<MyPayslipHistoryDto>> getMyPayslipHistory(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(payrollService.getMyPayslipHistory(pageable));
    }

    @PostMapping(path = "/{employeeId}/profile-picture", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Upload or replace an employee's profile picture")
    public ResponseEntity<CompleteEmployeeResponse> uploadProfilePicture(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @RequestParam("file") MultipartFile file) {
        CompleteEmployeeResponse response = employeeService.uploadProfilePicture(organizationId, employeeId, file);
        return ResponseEntity.ok(response);
    }
}