package com.aurionpro.papms.dto.payroll;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreatePayrollRequest {

    @NotNull
    @Min(value = 1, message = "Month must be between 1 and 12")
    @Max(value = 12, message = "Month must be between 1 and 12")
    private Integer payrollMonth;

    @NotNull
    @Min(value = 2020, message = "Year must be 2020 or later")
    private Integer payrollYear;
}