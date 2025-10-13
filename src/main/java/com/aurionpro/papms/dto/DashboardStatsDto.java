package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class DashboardStatsDto {

    // Main Balance
    private BigDecimal internalBalance;

    // Core Metrics from UI
    private Long totalEmployees;
    private Double employeeChangePercentage;
    private Long totalVendors;
    private Double vendorChangePercentage;
    private Long pendingInvoicesCount;
    private Double pendingInvoicesChangePercentage;

    // Client-related Financials
    private BigDecimal totalAmountReceivedFromClients; // this month
    private BigDecimal totalAmountDueFromClients; // outstanding

    // Vendor-related Financials
    private BigDecimal totalPaidToVendors; // this month

    // Transaction Summary
    private Long totalTransactions; // this month
    private BigDecimal totalTransactionVolume; // this month

    // Recent Activity
    private List<TransactionDto> recentTransactions;
}