package com.aurionpro.papms.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationProfileResponse {

    private Integer id;
    private String companyName;
    private String email;
    private String status;
}