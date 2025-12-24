package com.aurionpro.papms.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for payroll preview - shows employee salary details before confirmation.
 * Used in the preview step of payroll creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollPreviewDto {
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private String department;

    // Salary components from SalaryStructure
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal otherAllowances;
    private BigDecimal pfContribution;

    // Calculated totals
    private BigDecimal totalEarnings;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
}
