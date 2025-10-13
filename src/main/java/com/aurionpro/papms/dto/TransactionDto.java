package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.Enum.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionDto {
    private Long id;
    private LocalDateTime transactionDate;
    private TransactionType type;
    private BigDecimal amount;
    private String description;
    private BigDecimal balanceAfterTransaction;
    private TransactionSourceType sourceType;
    private Long sourceId; // e.g., The ID of the invoice, payroll batch, etc.
}