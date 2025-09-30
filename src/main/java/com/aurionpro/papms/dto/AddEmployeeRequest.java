package com.aurionpro.papms.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddEmployeeRequest {


    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    // Employee
    @NotBlank(message = "Employee Code is required")
    private String employeeCode;

    private LocalDate dateOfJoining;

    private String department;

    private String jobTitle;
}

//@Data
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class AddEmployeeRequest {
//
//    @NotBlank(message = "Username is required")
//    private String username;
//
//    @NotBlank(message = "Password is required")
//    private String password;
//
//    private String fullName;
//
//    @NotBlank(message = "Email is required")
//    @Email(message = "Invalid email format")
//    private String email;
//
//    @NotNull(message = "Organization ID is required")
//    private Integer organizationId;
//}
