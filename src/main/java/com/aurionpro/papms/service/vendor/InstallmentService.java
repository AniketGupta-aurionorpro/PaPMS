package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.dto.vendorDto.CreateInstallmentPlanRequest;
import com.aurionpro.papms.dto.vendorDto.InstallmentDto;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;

import java.util.List;

public interface InstallmentService {

    /**
     * Create an installment plan for a bill
     * 
     * @param request contains billId, numberOfInstallments, frequency,
     *                firstInstallmentDate
     * @return updated VendorBillDto with installments
     */
    VendorBillDto createInstallmentPlan(CreateInstallmentPlanRequest request);

    /**
     * Pay a specific installment
     * 
     * @param installmentId the installment to pay
     * @return updated VendorBillDto with updated payment status
     */
    VendorBillDto payInstallment(Long installmentId);

    /**
     * Get all installments for a bill
     * 
     * @param billId the bill ID
     * @return list of InstallmentDto
     */
    List<InstallmentDto> getInstallmentsForBill(Long billId);

    /**
     * Mark overdue installments (called by scheduler)
     */
    void markOverdueInstallments();
}
