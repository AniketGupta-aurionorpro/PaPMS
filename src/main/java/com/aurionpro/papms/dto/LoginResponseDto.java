package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // Important: Excludes null fields from the JSON
public class LoginResponseDto {

    private String accessToken;
    private String username;
    private String fullName;
    private String email;
    private Role role;

    // Organization details (for ORG_ADMIN, EMPLOYEE, CLIENT)
    private Integer organizationId;
    private String organizationName;

    // Complete profile for Employee role
    private CompleteEmployeeResponse employeeProfile;
}