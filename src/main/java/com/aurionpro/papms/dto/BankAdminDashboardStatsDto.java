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
    private double monthlyOrganizationGrowthPercentage; // ADDED
    private double monthlyTransactionVolumePercentage;

    @Data
    @Builder
    public static class OrganizationGrowthDataPoint {
        private String name; // e.g., "Jan 2024"
        private Integer value; // count of new orgs
    }

    @Data
    @Builder
    public static class TransactionVolumeDataPoint {
        private String name; // e.g., "Jan 2024"
        private BigDecimal value; // sum of transaction amounts
    }
}