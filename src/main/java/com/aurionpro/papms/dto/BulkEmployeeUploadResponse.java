package com.aurionpro.papms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmployeeUploadResponse {
    private int successfulImports;
    private int failedImports;
    private String message;
    private List<String> successfullyImportedEmployees; // e.g., list of names or emails
    private List<FailedEmployeeRecord> failedRecords;
}