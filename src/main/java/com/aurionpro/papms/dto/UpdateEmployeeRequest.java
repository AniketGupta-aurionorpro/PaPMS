// dto/UpdateEmployeeRequest.java
package com.aurionpro.papms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateEmployeeRequest {

    // User fields (updatable by employee themselves)
    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Employee fields (updatable by organization admin)
    private String department;
    private String jobTitle;

    // Bank Account fields (updatable by employee with verification)
    @Valid
    private UpdateBankAccountRequest bankAccount;

    @Data
    public static class UpdateBankAccountRequest {
        @NotBlank(message = "Account holder name is required")
        private String accountHolderName;

        @NotBlank(message = "Account number is required")
        private String accountNumber;

        @NotBlank(message = "Bank name is required")
        private String bankName;

        @NotBlank(message = "IFSC code is required")
        private String ifscCode;
    }
}