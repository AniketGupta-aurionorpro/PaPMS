package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.ConcernStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateConcernStatusRequest {
    @NotNull(message = "Status is required")
    private ConcernStatus status;

    // ADDED THIS FIELD
    private String resolutionNotes;
}