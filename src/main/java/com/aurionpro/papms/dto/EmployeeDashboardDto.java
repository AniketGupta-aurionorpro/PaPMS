package com.aurionpro.papms.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Excludes null fields (like latestPayslip if none exists)
public class EmployeeDashboardDto {

    // Contains all personal, bank, and current salary info
    private CompleteEmployeeResponse employeeProfile;

    // A summary of the most recent payslip
    private LatestPayslipDto latestPayslip;

    @Data
    @Builder
    public static class LatestPayslipDto {
        private Long paymentId;
        private String period; // e.g., "July 2024"
        private BigDecimal netSalary;
    }
}