// service/VendorService.java
package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorPaymentRequest;
import com.aurionpro.papms.dto.vendorDto.VendorRequest;
import com.aurionpro.papms.dto.vendorDto.VendorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface VendorService {
    VendorResponse createVendor(VendorRequest request);
    VendorResponse getVendorById(Long vendorId);
    //List<VendorResponse> getVendorsByOrganization(Integer organizationId);

    // MODIFIED: Method signature updated for pagination
    Page<VendorResponse> getVendorsByOrganization(Integer organizationId, Pageable pageable);

    VendorResponse updateVendor(Long vendorId, VendorRequest request);
    void deleteVendor(Long vendorId);
    void processVendorPayment(VendorPaymentRequest request);
}