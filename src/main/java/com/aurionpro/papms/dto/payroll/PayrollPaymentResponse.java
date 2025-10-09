package com.aurionpro.papms.dto.payroll;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PayrollPaymentResponse {
    private Long paymentId;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;

    // --- ADD THESE FIELDS FOR THE SALARY BREAKDOWN ---
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal otherAllowances;
    private BigDecimal pfContribution;
    private BigDecimal totalEarnings; // Calculated for convenience
    private BigDecimal totalDeductions; // Calculated for convenience
    // --- END OF ADDED FIELDS ---

    private BigDecimal netSalaryPaid;
    private String status;
}