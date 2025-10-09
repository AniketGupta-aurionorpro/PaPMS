package com.aurionpro.papms.dto.payroll;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PayrollBatchResponse {
    private Long id;
    private Integer organizationId;
    private String organizationName;
    private int payrollMonth;
    private int payrollYear;
    private BigDecimal totalAmount;
    private int totalEmployees;
    private String status;
    private String submittedBy;
    private String approvedBy;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private List<PayrollPaymentResponse> payments;
}