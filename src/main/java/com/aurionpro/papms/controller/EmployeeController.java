package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

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
    public ResponseEntity<List<EmployeeResponseDto>> getEmployeesByOrganization(
            @PathVariable Integer organizationId) {
        List<EmployeeResponseDto> employees = employeeService.getEmployeesByOrganization(organizationId);
        return ResponseEntity.ok(employees);
    }

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
}