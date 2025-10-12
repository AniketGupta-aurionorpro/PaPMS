package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.payroll.CreatePayrollRequest;
import com.aurionpro.papms.dto.payroll.PayrollBatchResponse;
import com.aurionpro.papms.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Payroll Management", description = "APIs for creating and processing monthly payrolls")
@Slf4j
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/organizations/{organizationId}/payrolls")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create and submit a new payroll batch for approval")
    public ResponseEntity<PayrollBatchResponse> createPayroll(
            @PathVariable Integer organizationId,
            @Valid @RequestBody CreatePayrollRequest request) {
        log.info("Request to create new payroll for org ID {} for month/year: {}/{}", organizationId, request.getPayrollMonth(), request.getPayrollYear());
        PayrollBatchResponse response = payrollService.createPayroll(organizationId, request);
        log.info("Successfully created payroll batch ID {} for org ID {}. Status: PENDING_APPROVAL", response.getId(), organizationId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/payrolls/pending")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Operation(summary = "Get all payroll batches pending approval")
    public ResponseEntity<Page<PayrollBatchResponse>> getPendingPayrolls(@ParameterObject Pageable pageable) {
        log.info("Bank Admin request to get all pending payrolls with pagination: {}", pageable);
        return ResponseEntity.ok(payrollService.getPendingPayrolls(pageable));
    }

    @GetMapping("/organizations/{organizationId}/payrolls")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get payroll history for an organization")
    public ResponseEntity<Page<PayrollBatchResponse>> getPayrollsForOrganization(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable) {
        log.info("Request to get payroll history for org ID {} with pagination: {}", organizationId, pageable);
        return ResponseEntity.ok(payrollService.getPayrollsForOrganization(organizationId, pageable));
    }

    @GetMapping("/payrolls/{batchId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    @Operation(summary = "Get detailed information for a specific payroll batch")
    public ResponseEntity<PayrollBatchResponse> getPayrollById(@PathVariable Long batchId) {
        log.info("Request to get details for payroll batch ID: {}", batchId);
        return ResponseEntity.ok(payrollService.getPayrollById(batchId));
    }

    @PutMapping("/payrolls/{batchId}/approve")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Operation(summary = "Approve a pending payroll batch")
    public ResponseEntity<PayrollBatchResponse> approvePayroll(@PathVariable Long batchId) {
        log.info("Bank Admin request to APPROVE payroll batch ID: {}", batchId);
        PayrollBatchResponse response = payrollService.approvePayroll(batchId);
        log.info("Successfully approved and processed payroll batch ID {}. New status: {}", batchId, response.getStatus());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/payrolls/{batchId}/reject")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Operation(summary = "Reject a pending payroll batch")
    public ResponseEntity<PayrollBatchResponse> rejectPayroll(
            @PathVariable Long batchId,
            @RequestBody String reason) {
        log.info("Bank Admin request to REJECT payroll batch ID: {}", batchId);
        PayrollBatchResponse response = payrollService.rejectPayroll(batchId, reason);
        log.info("Successfully rejected payroll batch ID {}. New status: {}", batchId, response.getStatus());
        return ResponseEntity.ok(response);
    }
}