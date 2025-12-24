// src/main/java/com/aurionpro/papms/dto/BankAdminDashboardStatsDto.java
package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BankAdminDashboardStatsDto {
    private long totalOrganizations;
    private double organizationGrowthPercentage;
    private long activeOrganizations;
    private long pendingOrganizations;
    private long suspendedOrganizations;
    private List<OrganizationGrowthDataPoint> organizationGrowth;
    private List<TransactionVolumeDataPoint> transactionVolume;
    private double monthlyOrganizationGrowthPercentage;
    private double monthlyTransactionVolumePercentage;

    // NEW: Payroll Trends
    private List<PayrollTrendDataPoint> payrollTrends;
    private double monthlyPayrollPercentage;

    // NEW: Transaction Count Trends (for tracking system activity)
    private List<TransactionCountDataPoint> transactionCounts;
    private double monthlyTransactionCountPercentage;

    @Data
    @Builder
    public static class OrganizationGrowthDataPoint {
        private String name; // e.g., "Jan'24"
        private Integer value; // count of new orgs
    }

    @Data
    @Builder
    public static class TransactionVolumeDataPoint {
        private String name; // e.g., "Jan'24"
        private BigDecimal value; // sum of transaction amounts
    }

    @Data
    @Builder
    public static class PayrollTrendDataPoint {
        private String name; // e.g., "Jan'24"
        private BigDecimal value; // total payroll amount processed
    }

    @Data
    @Builder
    public static class TransactionCountDataPoint {
        private String name; // e.g., "Jan'24"
        private Long value; // count of transactions (shows system activity/growth)
    }
}
