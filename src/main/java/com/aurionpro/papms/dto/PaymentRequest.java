package com.aurionpro.papms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequest {

    @NotNull(message = "Invoice ID is required for payment")
    private Integer invoiceId;

    // Could be expanded with payment method, partial payment amount, etc.
}