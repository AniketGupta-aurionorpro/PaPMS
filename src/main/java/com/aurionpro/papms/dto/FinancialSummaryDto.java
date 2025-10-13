package com.aurionpro.papms.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class FinancialSummaryDto {
    private Integer organizationId;
    private String organizationName;
    private String organizationStatus;
    private BigDecimal currentBalance;
    private BigDecimal totalCredits;
    private BigDecimal totalDebits;
    private long totalTransactions;
    private LocalDateTime firstTransactionDate;
    private LocalDateTime lastTransactionDate;
}