package com.aurionpro.papms.dto.vendorDto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class VendorBillDto {
    private Long id;
    private String billNumber;
    private Long vendorPaymentId;
    private Long vendorId;
    private String vendorName;
    private Integer organizationId;
    private String organizationName;
    private BigDecimal amount;
    private BigDecimal paidAmount;
    private BigDecimal dueAmount;
    private LocalDate billDate;
    private LocalDate dueDate;
    private String status;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Installment fields
    private java.util.List<InstallmentDto> installments;
    private Integer totalInstallments;
    private Integer paidInstallments;
    private String installmentFrequency;
}