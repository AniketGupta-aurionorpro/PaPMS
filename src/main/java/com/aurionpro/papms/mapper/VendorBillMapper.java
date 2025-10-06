package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.vendorEntity.VendorBill;

public class VendorBillMapper {

    public static VendorBillDto toDto(VendorBill bill) {
        return VendorBillDto.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .vendorPaymentId(bill.getVendorPayment().getId())
                .vendorId(bill.getVendor().getId())
                .vendorName(bill.getVendor().getVendorName())
                .organizationName(bill.getOrganization().getCompanyName()) // <-- THIS IS THE FIX
                .amount(bill.getAmount())
                .billDate(bill.getBillDate())
                .status("PAID") //  sent -/ paid.
                .createdAt(bill.getCreatedAt())
                .build();
    }
}