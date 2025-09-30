package com.aurionpro.papms.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationRegistrationReq {

    @NotBlank(message = "Company name cannot be blank")
    private String companyName;

    @NotBlank(message = "Username cannot be blank")
    private String username;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String fullName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    private String address;

    @Max(15)
    @Min(10)
    @NotBlank(message = "Enter a Phone number to contact")
    private String contactNumber;


    public String getContactPhone() {
        return contactNumber;
    }
}