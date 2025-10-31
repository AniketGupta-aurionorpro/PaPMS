package com.aurionpro.papms.dto;

import jakarta.validation.Valid;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateCompleteEmployeeRequest {

    @Valid
    private UserDetails user;

    @Valid
    private EmployeeDetails employee;

    @Valid
    private BankAccountDetails bankAccount;

    @Valid
    private SalaryDetails salary;

    @Data
    public static class UserDetails {
        private String fullName;
        private String email;
    }

    @Data
    public static class EmployeeDetails {
        private String department;
        private String jobTitle;
        private Boolean isActive; // For activating/deactivating
    }

    @Data
    public static class BankAccountDetails {
        private String accountHolderName;
        private String accountNumber;
        private String bankName;
        private String ifscCode;
    }

    @Data
    public static class SalaryDetails {
        private BigDecimal basicSalary;
        private BigDecimal hra;
        private BigDecimal da;
        private BigDecimal pfContribution;
        private BigDecimal otherAllowances;
        private LocalDate effectiveFromDate;
        private String changeReason;
    }
}