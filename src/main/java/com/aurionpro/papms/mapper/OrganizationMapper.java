// papms/mapper/OrganizationMapper.java
package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.OrganizationResponseDto;
import com.aurionpro.papms.dto.OrganizationResponseDtowithEmployee;
import com.aurionpro.papms.entity.Organization;

import java.util.stream.Collectors;

public class OrganizationMapper {

    public static OrganizationResponseDto toDto(Organization organization) {
        OrganizationResponseDto dto = new OrganizationResponseDto();
        dto.setId(organization.getId());
        dto.setCompanyName(organization.getCompanyName());
        dto.setAddress(organization.getAddress());
        dto.setContactEmail(organization.getContactEmail());
        dto.setLogoUrl(organization.getLogoUrl());
        dto.setStatus(organization.getStatus());
        dto.setBankAssignedAccountNumber(organization.getBankAssignedAccountNumber());
        dto.setInternalBalance(organization.getInternalBalance());
        dto.setCreatedAt(organization.getCreatedAt());

//        dto.setEmployees(organization.getEmployees().stream()
//                .map(EmployeeMapper::toDto)
//                .collect(Collectors.toList()));

        if (organization.getDocuments() != null) {
            dto.setDocuments(organization.getDocuments().stream()
                    .map(DocumentMapper::fromEntity)
                    .collect(Collectors.toList()));
        }
        return dto;
    }

    // Simple DTO without nested collections for list views
    public static OrganizationResponseDto toSimpleDto(Organization organization) {
        OrganizationResponseDto dto = new OrganizationResponseDto();
        dto.setId(organization.getId());
        dto.setCompanyName(organization.getCompanyName());
        dto.setAddress(organization.getAddress());
        dto.setContactEmail(organization.getContactEmail());
        dto.setLogoUrl(organization.getLogoUrl());
        dto.setStatus(organization.getStatus());
        dto.setBankAssignedAccountNumber(organization.getBankAssignedAccountNumber());
        dto.setInternalBalance(organization.getInternalBalance());
        dto.setCreatedAt(organization.getCreatedAt());
        return dto;
    }
    public static OrganizationResponseDtowithEmployee toDtoWithEmployees(Organization organization) {
        OrganizationResponseDtowithEmployee dto = new OrganizationResponseDtowithEmployee();
        dto.setId(organization.getId());
        dto.setCompanyName(organization.getCompanyName());
        dto.setAddress(organization.getAddress());
        dto.setContactEmail(organization.getContactEmail());
        dto.setLogoUrl(organization.getLogoUrl());
        dto.setStatus(organization.getStatus());
        dto.setBankAssignedAccountNumber(organization.getBankAssignedAccountNumber());
        dto.setInternalBalance(organization.getInternalBalance());
        dto.setCreatedAt(organization.getCreatedAt());

        // Map documents
        if (organization.getDocuments() != null) {
            dto.setDocuments(organization.getDocuments().stream()
                    .map(DocumentMapper::fromEntity)
                    .collect(Collectors.toList()));
        }

        // Map employees
        if (organization.getEmployees() != null) {
            dto.setEmployees(organization.getEmployees().stream()
                    .map(EmployeeMapper::toDto)
                    .collect(Collectors.toList()));
        }

        return dto;
    }
}