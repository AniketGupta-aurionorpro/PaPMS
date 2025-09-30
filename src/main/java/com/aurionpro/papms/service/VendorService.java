// service/VendorService.java
package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.VendorPaymentRequest;
import com.aurionpro.papms.dto.VendorRequest;
import com.aurionpro.papms.dto.VendorResponse;
import java.util.List;

public interface VendorService {
    VendorResponse createVendor(VendorRequest request);
    VendorResponse getVendorById(Long vendorId);
    List<VendorResponse> getVendorsByOrganization(Integer organizationId);
    VendorResponse updateVendor(Long vendorId, VendorRequest request);
    void deleteVendor(Long vendorId);
    void processVendorPayment(VendorPaymentRequest request);
}