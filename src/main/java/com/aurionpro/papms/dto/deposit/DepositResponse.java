package com.aurionpro.papms.dto.deposit;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;


@Data
@Builder
public class DepositResponse {
    private Long id;
    private Integer organizationId;
    private String organizationName;
    private BigDecimal amountDeposited;
    private BigDecimal balanceAfterDeposit;
    private LocalDateTime depositTimestamp;
}