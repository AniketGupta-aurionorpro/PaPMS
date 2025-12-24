package com.aurionpro.papms.dto.vendorDto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInstallmentPlanRequest {

    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Number of installments is required")
    @Min(value = 2, message = "Minimum 2 installments required")
    private Integer numberOfInstallments;

    @NotNull(message = "Frequency is required")
    private String frequency; // WEEKLY, BI_WEEKLY, MONTHLY

    @NotNull(message = "First installment date is required")
    private LocalDate firstInstallmentDate;
}
