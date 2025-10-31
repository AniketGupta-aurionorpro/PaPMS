package com.aurionpro.papms.dto;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;

@Getter
@Setter
public class EmployeeResponseDto {


    private Long id; // This is the user's ID
    private String username;
    private String fullName;
    private String email;
    private String role;
    private boolean isUserEnabled;


    private Integer organizationId;
    private String organizationName;

    // Employee-specific fields
    private String employeeCode;
    private LocalDate dateOfJoining;
    private String department;
    private String jobTitle;
    @JsonProperty("isEmployeeActive")
    private boolean isEmployeeActive;
}

