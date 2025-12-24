package com.aurionpro.papms.dto.vendorDto;

import com.aurionpro.papms.Enum.BillStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BillPaymentRequest {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @DecimalMin(value = "0", message = "Amount cannot be negative")
    private BigDecimal amount;

    @NotNull(message = "Payment mode is required")
    private String paymentMode; // FULL, PARTIAL, PAY_LATER

    private String description;

    private LocalDate payLaterDate;
}
