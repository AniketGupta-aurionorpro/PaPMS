package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.payroll.CreatePayrollRequest;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.dto.payroll.PayrollBatchResponse;
import com.aurionpro.papms.dto.payroll.PayrollPreviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;

import java.util.List;
import java.util.Map;

public interface PayrollService {
    PayrollBatchResponse createPayroll(Integer organizationId, CreatePayrollRequest request);

    Page<PayrollBatchResponse> getPayrollsForOrganization(Integer organizationId, Pageable pageable);

    Page<PayrollBatchResponse> getPendingPayrolls(Pageable pageable);

    PayrollBatchResponse getPayrollById(Long batchId);

    PayrollBatchResponse approvePayroll(Long batchId);

    PayrollBatchResponse rejectPayroll(Long batchId, String reason);

    PayrollPaymentResponse getPayrollPaymentDetails(Long paymentId);

    Page<MyPayslipHistoryDto> getMyPayslipHistory(Pageable pageable);

    Map<Integer, Long> getPendingPayrollCountsByOrganization();

    List<PayrollBatchResponse> getPayrollsByYear(Integer organizationId, int year);

    /**
     * Get payroll preview for a given month/year showing all employees and their
     * salaries.
     */
    List<PayrollPreviewDto> getPayrollPreview(Integer organizationId, Integer month, Integer year);
}