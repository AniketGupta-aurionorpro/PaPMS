package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.payroll.CreatePayrollRequest;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.dto.payroll.PayrollBatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;

public interface PayrollService {
    PayrollBatchResponse createPayroll(Integer organizationId, CreatePayrollRequest request);
    Page<PayrollBatchResponse> getPayrollsForOrganization(Integer organizationId, Pageable pageable);
    Page<PayrollBatchResponse> getPendingPayrolls(Pageable pageable);
    PayrollBatchResponse getPayrollById(Long batchId);
    PayrollBatchResponse approvePayroll(Long batchId);
    PayrollBatchResponse rejectPayroll(Long batchId, String reason);
    PayrollPaymentResponse getPayrollPaymentDetails(Long paymentId);
    Page<MyPayslipHistoryDto> getMyPayslipHistory(Pageable pageable);
}