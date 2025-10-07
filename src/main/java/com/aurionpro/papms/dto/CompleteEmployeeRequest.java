// dto/CompleteEmployeeRequest.java
package com.aurionpro.papms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CompleteEmployeeRequest {

    // User details
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Employee details
    @NotBlank(message = "Employee Code is required")
    private String employeeCode;

    @NotNull(message = "Date of joining is required")
    private LocalDate dateOfJoining;

    private String department;

    private String jobTitle;

    // Bank Account details
    @Valid
    @NotNull(message = "Bank account details are required")
    private BankAccountRequest bankAccount;

    // Salary Structure details
    @Valid
    @NotNull(message = "Salary structure details are required")
    private SalaryStructureRequest salaryStructure;

    @Data
    public static class BankAccountRequest {
        @NotBlank(message = "Account holder name is required")
        private String accountHolderName;

        @NotBlank(message = "Account number is required")
        @Size(min = 5, max = 50, message = "Account number must be between 5 and 50 characters")
        private String accountNumber;

        @NotBlank(message = "Bank name is required")
        private String bankName;

        @NotBlank(message = "IFSC code is required")
        @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "Invalid IFSC code format")
        private String ifscCode;
    }

    @Data
    public static class SalaryStructureRequest {
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
    }
}