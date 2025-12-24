package com.aurionpro.papms.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs for Client Transaction operations
 */
public class ClientTransactionDto {

    /**
     * Single transaction record for client
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionRecord {
        private Long id;
        private String type; // DEPOSIT_REQUEST, INVOICE_PAYMENT
        private String description;
        private BigDecimal amount;
        private String status;
        private LocalDateTime transactionDate;
        private BigDecimal balanceAfter;
        private String referenceNumber;
    }

    /**
     * Response containing list of transactions
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TransactionHistoryResponse {
        private List<TransactionRecord> transactions;
        private BigDecimal currentBalance;
        private BigDecimal totalDeposited;
        private BigDecimal totalSpent;
    }
}
