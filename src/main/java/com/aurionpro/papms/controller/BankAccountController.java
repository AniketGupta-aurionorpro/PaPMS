package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.BankAccountResponse;
import com.aurionpro.papms.service.BankAccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/organizations/{organizationId}/bank-accounts")
@RequiredArgsConstructor
@Tag(name = "Bank Account Management", description = "APIs for managing employee bank accounts")
public class BankAccountController {

    private final BankAccountService bankAccountService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get all bank accounts for organization",
            description = "ORG_ADMIN: view all accounts, EMPLOYEE: view own accounts only")
    public ResponseEntity<List<BankAccountResponse>> getBankAccounts(
            @PathVariable Integer organizationId) {
        List<BankAccountResponse> accounts = bankAccountService.getBankAccountsByOrganization(organizationId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get specific bank account details")
    public ResponseEntity<BankAccountResponse> getBankAccountById(
            @PathVariable Integer organizationId,
            @PathVariable Long accountId) {
        BankAccountResponse account = bankAccountService.getBankAccountById(organizationId, accountId);
        return ResponseEntity.ok(account);
    }

    @GetMapping("/employees/{employeeId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Get bank accounts for specific employee",
            description = "ORG_ADMIN: view any employee, EMPLOYEE: view own accounts only")
    public ResponseEntity<List<BankAccountResponse>> getEmployeeBankAccounts(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId) {
        List<BankAccountResponse> accounts = bankAccountService.getEmployeeBankAccounts(organizationId, employeeId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/employees/{employeeId}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create bank account for employee")
    public ResponseEntity<BankAccountResponse> createEmployeeBankAccount(
            @PathVariable Integer organizationId,
            @PathVariable Long employeeId,
            @RequestBody BankAccountResponse request) {
        BankAccountResponse response = bankAccountService.createEmployeeBankAccount(organizationId, employeeId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{accountId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'EMPLOYEE')")
    @Operation(summary = "Update bank account details")
    public ResponseEntity<BankAccountResponse> updateBankAccount(
            @PathVariable Integer organizationId,
            @PathVariable Long accountId,
            @RequestBody BankAccountResponse request) {
        BankAccountResponse response = bankAccountService.updateBankAccount(organizationId, accountId, request);
        return ResponseEntity.ok(response);
    }
}