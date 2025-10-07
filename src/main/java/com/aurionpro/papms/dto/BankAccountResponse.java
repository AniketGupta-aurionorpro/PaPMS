package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankAccountResponse {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String accountHolderName;
    private String accountNumber; // Will be masked
    private String bankName;
    private String ifscCode;
    private boolean isPrimary;
}