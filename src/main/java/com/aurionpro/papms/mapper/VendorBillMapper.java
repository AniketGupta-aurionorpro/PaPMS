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
                .amount(bill.getAmount())
                .billDate(bill.getBillDate())
                .status("SENT") // Hardcoded: Its existence implies it's sent.
                .createdAt(bill.getCreatedAt())
                .build();
    }
}