package com.aurionpro.papms.dto;

import com.aurionpro.papms.Enum.ClientDepositStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTOs for Client Deposit Request operations
 */
public class ClientDepositRequestDto {

    /**
     * Request DTO for creating a new deposit request
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {
        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Amount must be at least ₹1")
        private BigDecimal amount;

        private String referenceNumber;
        private String remarks;
    }

    /**
     * Request DTO for approving/rejecting a deposit
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ProcessRequest {
        private String rejectionReason;
    }

    /**
     * Response DTO for deposit request details
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private Long id;
        private Long clientId;
        private String clientName;
        private String clientEmail;
        private Integer organizationId;
        private String organizationName;
        private BigDecimal amount;
        private String referenceNumber;
        private String remarks;
        private ClientDepositStatus status;
        private String rejectionReason;
        private LocalDateTime processedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    /**
     * Summary DTO for dashboard
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Summary {
        private long pendingCount;
        private long approvedCount;
        private long rejectedCount;
        private BigDecimal totalPendingAmount;
        private BigDecimal totalApprovedAmount;
    }
}
