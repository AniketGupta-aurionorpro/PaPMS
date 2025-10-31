package com.aurionpro.papms.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobReportDto {

    private Long id;
    private Long jobExecutionId;
    private String organizationName;
    private String jobName;
    private String status;
    private int totalRecordsRead;
    private int successfulImports;
    private int failedImports;
    private LocalDateTime createdAt;
    private List<FailedEmployeeRecord> failedRecords;

    public static List<FailedEmployeeRecord> deserializeReportDetails(String jsonDetails) {
        if (jsonDetails == null || jsonDetails.isBlank()) {
            return Collections.emptyList();
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(jsonDetails, new TypeReference<>() {});
        } catch (Exception e) {
            // Return a single record indicating the deserialization error
            return List.of(new FailedEmployeeRecord(0, Collections.emptyMap(), "Error parsing report details: " + e.getMessage()));
        }
    }
}