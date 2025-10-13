package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ConcernResponseDto {
    private Long id;
    private String subject;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ADDED THIS FIELD
    private String resolutionNotes;

    // Employee Info
    private Long employeeId;
    private String employeeName;

    // Admin Info (who handled it)
    private String resolvedByAdminName;
}