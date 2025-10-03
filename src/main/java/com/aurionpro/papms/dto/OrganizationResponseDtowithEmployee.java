// papms/dto/OrganizationResponseDto.java
package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.OrganizationStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrganizationResponseDtowithEmployee {
    private Integer id;
    private String companyName;
    private String address;
    private String contactEmail;
    private OrganizationStatus status;
    private String logoUrl;
    private String bankAssignedAccountNumber;
    private BigDecimal internalBalance;
    private LocalDateTime createdAt;
    private List<EmployeeResponseDto> employees;
    private List<DocumentResponseDto> documents;


}