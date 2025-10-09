package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.payroll.PayrollBatchResponse;
import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;
import com.aurionpro.papms.entity.PayrollBatch;
import com.aurionpro.papms.entity.PayrollPayment;

import java.math.BigDecimal; // Import BigDecimal
import java.util.stream.Collectors;

public class PayrollMapper {

    public static PayrollBatchResponse toDto(PayrollBatch batch) {
        return PayrollBatchResponse.builder()
                .id(batch.getId())
                .organizationId(batch.getOrganization().getId())
                .organizationName(batch.getOrganization().getCompanyName())
                .payrollMonth(batch.getPayrollMonth())
                .payrollYear(batch.getPayrollYear())
                .totalAmount(batch.getTotalAmount())
                .totalEmployees(batch.getTotalEmployees())
                .status(batch.getStatus().name())
                .submittedBy(batch.getSubmittedByUser() != null ? batch.getSubmittedByUser().getFullName() : "N/A")
                .approvedBy(batch.getApprovedByUser() != null ? batch.getApprovedByUser().getFullName() : "N/A")
                .rejectionReason(batch.getRejectionReason())
                .createdAt(batch.getCreatedAt())
                .payments(batch.getPayments().stream().map(PayrollMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    public static PayrollPaymentResponse toDto(PayrollPayment payment) {
        // --- MODIFIED: Calculate totals for clarity ---
        BigDecimal totalEarnings = payment.getBasicSalary()
                .add(payment.getHra())
                .add(payment.getDa())
                .add(payment.getOtherAllowances());

        BigDecimal totalDeductions = payment.getPfContribution();

        return PayrollPaymentResponse.builder()
                .paymentId(payment.getId())
                .employeeId(payment.getEmployee().getId())
                .employeeName(payment.getEmployee().getUser().getFullName())
                .employeeCode(payment.getEmployee().getEmployeeCode())

                // --- MODIFIED: Map the new salary breakdown fields ---
                .basicSalary(payment.getBasicSalary())
                .hra(payment.getHra())
                .da(payment.getDa())
                .otherAllowances(payment.getOtherAllowances())
                .pfContribution(payment.getPfContribution())
                .totalEarnings(totalEarnings)
                .totalDeductions(totalDeductions)
                // --- END OF MODIFICATIONS ---

                .netSalaryPaid(payment.getNetSalaryPaid())
                .status(payment.getStatus().name())
                .build();
    }
}