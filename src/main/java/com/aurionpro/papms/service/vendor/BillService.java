package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.vendorEntity.VendorPayment;

import java.util.List;

public interface BillService {
    VendorBillDto generateBillForPayment(VendorPayment payment);
    VendorBillDto getBillById(Long billId);
    List<VendorBillDto> getAllBillsForOrganization();
}