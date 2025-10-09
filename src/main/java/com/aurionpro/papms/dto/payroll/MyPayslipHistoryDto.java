package com.aurionpro.papms.dto.payroll;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class MyPayslipHistoryDto {
    private Long paymentId; // The most important field!
    private int payrollMonth;
    private int payrollYear;
    private BigDecimal netSalaryPaid;
    private String status;
}