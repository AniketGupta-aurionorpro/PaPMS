// src/main/java/com/aurionpro/papms/dto/BankAdminDashboardStatsDto.java
package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class BankAdminDashboardStatsDto {
    private long totalOrganizations;
    private long activeOrganizations;
    private long pendingOrganizations;
    private long suspendedOrganizations;
    private List<OrganizationGrowthDataPoint> organizationGrowth;

    @Data
    @Builder
    public static class OrganizationGrowthDataPoint {
        private String name; // e.g., "Jan 2024"
        private Integer value; // count of new orgs
    }
}