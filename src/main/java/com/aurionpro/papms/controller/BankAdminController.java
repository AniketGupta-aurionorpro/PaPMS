package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.FinancialSummaryDto;
import com.aurionpro.papms.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bank-admin")
@RequiredArgsConstructor
@Tag(name = "Bank Admin Tools", description = "APIs for Bank Administrators to audit and manage organizations")
public class BankAdminController {

    private final DashboardService dashboardService;

    @GetMapping("/organizations/{organizationId}/financial-summary")
    @PreAuthorize("hasRole('BANK_ADMIN')")
    @Operation(summary = "Get a financial audit summary for an organization",
            description = "Provides a high-level financial overview including balance, total credits/debits, and transaction counts for a specific organization.")
    public ResponseEntity<FinancialSummaryDto> getFinancialSummary(
            @PathVariable Integer organizationId) {
        FinancialSummaryDto summary = dashboardService.getFinancialSummaryForBankAdmin(organizationId);
        return ResponseEntity.ok(summary);
    }
}