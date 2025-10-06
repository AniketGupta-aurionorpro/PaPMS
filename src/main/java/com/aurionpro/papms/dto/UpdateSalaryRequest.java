// dto/UpdateSalaryRequest.java
package com.aurionpro.papms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateSalaryRequest {
    @NotNull(message = "Basic salary is required")
    @DecimalMin(value = "0.01", message = "Basic salary must be greater than zero")
    private BigDecimal basicSalary;

    @DecimalMin(value = "0.00", message = "HRA cannot be negative")
    private BigDecimal hra = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "DA cannot be negative")
    private BigDecimal da = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "PF contribution cannot be negative")
    private BigDecimal pfContribution = BigDecimal.ZERO;

    @DecimalMin(value = "0.00", message = "Other allowances cannot be negative")
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @NotNull(message = "Effective from date is required")
    private LocalDate effectiveFromDate;

    private String changeReason; // For audit purposes
}