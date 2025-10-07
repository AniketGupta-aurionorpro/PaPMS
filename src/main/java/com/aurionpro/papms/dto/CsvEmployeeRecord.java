// dto/CsvEmployeeRecord.java
package com.aurionpro.papms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CsvEmployeeRecord {
    // User fields
    private String username;
    private String password;
    private String fullName;
    private String email;

    // Employee fields
    private String employeeCode;
    private LocalDate dateOfJoining;
    private String department;
    private String jobTitle;

    // Bank Account fields
    private String accountHolderName;
    private String accountNumber;
    private String bankName;
    private String ifscCode;

    // Salary Structure fields
    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal da;
    private BigDecimal pfContribution;
    private BigDecimal otherAllowances;
    private LocalDate effectiveFromDate;

    public CsvEmployeeRecord() {
    }
}