package com.aurionpro.papms.mapper;

import com.aurionpro.papms.Enum.InstallmentStatus;
import com.aurionpro.papms.dto.vendorDto.InstallmentDto;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.vendorEntity.BillInstallment;
import com.aurionpro.papms.entity.vendorEntity.VendorBill;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class VendorBillMapper {

    public static VendorBillDto toDto(VendorBill bill) {
        BigDecimal paidAmount = bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO;
        BigDecimal amount = bill.getAmount() != null ? bill.getAmount() : BigDecimal.ZERO;
        BigDecimal dueAmount = amount.subtract(paidAmount);

        // Map installments
        List<InstallmentDto> installmentDtos = Collections.emptyList();
        int paidInstallments = 0;

        if (bill.getInstallments() != null && !bill.getInstallments().isEmpty()) {
            installmentDtos = bill.getInstallments().stream()
                    .map(VendorBillMapper::toInstallmentDto)
                    .collect(Collectors.toList());
            paidInstallments = (int) bill.getInstallments().stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                    .count();
        }

        return VendorBillDto.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .vendorPaymentId(bill.getVendorPayment() != null ? bill.getVendorPayment().getId() : null)
                .vendorId(bill.getVendor().getId())
                .vendorName(bill.getVendor().getVendorName())
                .organizationId(bill.getOrganization().getId())
                .organizationName(bill.getOrganization().getCompanyName())
                .amount(amount)
                .paidAmount(paidAmount)
                .dueAmount(dueAmount)
                .billDate(bill.getBillDate())
                .dueDate(bill.getDueDate())
                .status(bill.getStatus() != null ? bill.getStatus().name() : "PENDING")
                .description(bill.getDescription())
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                // Installment fields
                .installments(installmentDtos)
                .totalInstallments(bill.getTotalInstallments())
                .paidInstallments(paidInstallments)
                .installmentFrequency(
                        bill.getInstallmentFrequency() != null ? bill.getInstallmentFrequency().name() : null)
                .build();
    }

    public static InstallmentDto toInstallmentDto(BillInstallment installment) {
        return InstallmentDto.builder()
                .id(installment.getId())
                .billId(installment.getBill().getId())
                .installmentNumber(installment.getInstallmentNumber())
                .amount(installment.getAmount())
                .dueDate(installment.getDueDate())
                .paidDate(installment.getPaidDate())
                .status(installment.getStatus().name())
                .createdAt(installment.getCreatedAt())
                .updatedAt(installment.getUpdatedAt())
                .build();
    }
}