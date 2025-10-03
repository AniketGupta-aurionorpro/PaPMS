package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponseDto {

    private Integer id;
    private String invoiceNumber;
    private BigDecimal amount;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private LocalDateTime paidAt;

    // Associated entity information
    private Integer organizationId;
    private String organizationName;
    private Integer clientId;
    private String clientCompanyName;
    private String clientContactPerson;
}