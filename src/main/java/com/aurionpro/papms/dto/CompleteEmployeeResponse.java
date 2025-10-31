// dto/CompleteEmployeeResponse.java
package com.aurionpro.papms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonProperty;

@Data
public class CompleteEmployeeResponse {
    private Long id;
    private String username;
    private String fullName;
    private String email;
    private String role;
    private boolean isUserEnabled;
    private String profilePictureUrl;

    private Integer organizationId;
    private String organizationName;

    // Employee-specific fields
    private String employeeCode;
    private LocalDate dateOfJoining;
    private String department;
    private String jobTitle;
    @JsonProperty("isEmployeeActive") // Add this annotation
    private boolean isEmployeeActive;

    // Bank Account details
    private BankAccountResponse bankAccount;

    // Salary Structure details
    private SalaryStructureResponse currentSalary;

    private LocalDateTime createdAt;

    @Data
    public static class BankAccountResponse {
        private Integer id;
        private String accountHolderName;
        private String accountNumber;
        private String bankName;
        private String ifscCode;
        private boolean isPrimary;
    }

    @Data
    public static class SalaryStructureResponse {
        private Long id;
        private BigDecimal basicSalary;
        private BigDecimal hra;
        private BigDecimal da;
        private BigDecimal pfContribution;
        private BigDecimal otherAllowances;
        private BigDecimal totalSalary;
        private LocalDate effectiveFromDate;
        private boolean isActive;
    }
}