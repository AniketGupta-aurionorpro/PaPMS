package com.aurionpro.papms.dto.payroll;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO for overriding an employee's salary during payroll creation.
 * Only modified employees need to be included in the list.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryOverrideDto {
    private Long employeeId;
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal otherAllowances;
    private BigDecimal pfContribution;
}
