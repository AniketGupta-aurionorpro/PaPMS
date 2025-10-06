// dto/BulkEmployeeUploadResponse.java - Update with complete employee info
package com.aurionpro.papms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeeUploadResponse {
    private int successfulImports;
    private int failedImports;
    private String message;
    private List<String> successfullyImportedEmployees;
    private List<FailedEmployeeRecord> failedRecords;
    private List<CompleteEmployeeResponse> importedEmployees; // New field for complete data

    // Summary statistics
    private BigDecimal totalMonthlySalary;
    private int employeesWithBankAccounts;
    private int employeesWithSalaryStructure;
}