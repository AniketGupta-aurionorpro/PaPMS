package com.aurionpro.papms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

/**
 * DTOs for Invoice Payment operations
 */
public class InvoicePaymentDto {

    /**
     * Request DTO for paying an invoice
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentRequest { // Request DTO
        private Integer invoiceId;

        @NotNull(message = "Payment mode is required")
        private String paymentMode; // FULL, PARTIAL

        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount; // Required for PARTIAL mode

        private String description;
    }

    /**
     * Response DTO for payment result
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentResponse {
        private Integer invoiceId;
        private String invoiceNumber;
        private BigDecimal totalAmount;
        private BigDecimal paidAmount;
        private BigDecimal remainingAmount;
        private String status;
        private BigDecimal clientBalanceAfter;
        private String message;
    }
}
