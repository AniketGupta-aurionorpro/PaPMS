package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.BulkEmployeeUploadResponse;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import com.aurionpro.papms.service.EmployeeService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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


    @GetMapping("/username/{username}")
    public ResponseEntity<EmployeeResponseDto> getEmployeeByUsername(@PathVariable String username) {
        return ResponseEntity.ok(employeeService.getEmployeeByUsername(username));
    }

}