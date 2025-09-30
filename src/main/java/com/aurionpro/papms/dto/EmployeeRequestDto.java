package com.aurionpro.papms.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeRequestDto {
    @NotBlank
    private String username;
//more feilds like employee table ,
//mapper me ( divide in 2 , ek user enitty, ek employee enity)
    //try to avid redundant data
    //joiing data date and username
    @NotBlank
    private String password;

    @NotBlank
    private String fullName;

    @Email
    @NotBlank
    private String email;

    //ye nahi cheye service me dal
    @NotNull
    private Long organizationId;
}

