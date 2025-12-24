package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@Builder
public class ClientResponseDto {
    // Client-specific fields
    private Integer clientId;
    private String companyName;
    private String contactPerson;
    @JsonProperty("isClientActive")
    private boolean isClientActive;

    // User-specific fields
    private Long userId;
    private String username;
    private String email;
    private String fullName;
    @JsonProperty("isUserActive")
    private boolean isUserActive;

    // Organization info
    private Integer organizationId;
    private String organizationName;

    private LocalDateTime createdAt;
}