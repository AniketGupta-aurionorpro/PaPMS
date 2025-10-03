// mapper/VendorMapper.java
package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.vendorDto.VendorRequest;
import com.aurionpro.papms.dto.vendorDto.VendorResponse;
import com.aurionpro.papms.entity.BankAccount;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.vendorEntity.Vendor;

public class VendorMapper {
    public static Vendor toEntity(VendorRequest request, Organization organization) {
        return Vendor.builder()
                .vendorName(request.getVendorName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .organization(organization)
                .build();
    }

    public static VendorResponse toDto(Vendor vendor, BankAccount bankAccount) {
        VendorResponse dto = new VendorResponse();
        dto.setId(vendor.getId());
        dto.setVendorName(vendor.getVendorName());
        dto.setContactEmail(vendor.getContactEmail());
        dto.setContactPhone(vendor.getContactPhone());
        dto.setAddress(vendor.getAddress());
        dto.setIsActive(vendor.getIsActive());
        dto.setOrganizationId(vendor.getOrganization().getId());

        if (bankAccount != null) {
            dto.setAccountHolderName(bankAccount.getAccountHolderName());
            dto.setAccountNumber(bankAccount.getAccountNumber());
            dto.setBankName(bankAccount.getBankName());
            dto.setIfscCode(bankAccount.getIfscCode());
        }
        return dto;
    }

    public static void updateEntityFromRequest(Vendor vendor, VendorRequest request) {
        vendor.setVendorName(request.getVendorName());
        vendor.setContactEmail(request.getContactEmail());
        vendor.setContactPhone(request.getContactPhone());
        vendor.setAddress(request.getAddress());
    }
}
