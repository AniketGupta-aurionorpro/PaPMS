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
    private BigDecimal amount;
    private LocalDate billDate;
    private String status;
    private LocalDateTime createdAt;
}