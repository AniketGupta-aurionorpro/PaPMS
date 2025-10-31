package com.aurionpro.papms.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class ReportRequestDto {
    private String reportType;
    private String organizationId; // Can be "ALL" or a specific ID
    private LocalDate startDate;
    private LocalDate endDate;
}