package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.dto.vendorDto.BillPaymentRequest;
import com.aurionpro.papms.dto.vendorDto.CreateBillRequest;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import com.aurionpro.papms.entity.vendorEntity.VendorPayment;

import java.util.List;

public interface BillService {
    // Generate bill after payment (existing)
    VendorBillDto generateBillForPayment(VendorPayment payment, Organization organization, Vendor vendor);

    // Get bill by ID
    VendorBillDto getBillById(Long billId);

    // Get all bills for org admin's organization
    List<VendorBillDto> getAllBillsForOrganization();

    // Get all bills for logged-in vendor
    List<VendorBillDto> getAllBillsForVendor();

    // Vendor creates a new bill
    VendorBillDto createBill(CreateBillRequest request);

    // Org admin pays a bill
    VendorBillDto payBill(BillPaymentRequest request);
}