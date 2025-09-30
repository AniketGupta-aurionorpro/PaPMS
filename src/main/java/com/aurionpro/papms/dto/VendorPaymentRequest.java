// dto/VendorPaymentRequest.java
package com.aurionpro.papms.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class VendorPaymentRequest {
    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    private BigDecimal amount;

    private String description;
}