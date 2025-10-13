package com.aurionpro.papms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RaiseConcernRequest {
    @NotBlank(message = "Subject is required")
    @Size(max = 255, message = "Subject cannot be longer than 255 characters")
    private String subject;

    @NotBlank(message = "Description is required")
    private String description;
}