package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorRequest;
import com.aurionpro.papms.dto.vendorDto.VendorResponse;
import com.aurionpro.papms.service.vendor.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    // convert the JSON from the request body into a VendorRequest object
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody VendorRequest vendorRequest) {
        //VendorResponse DTO is stored in a local variable called newVendor
        VendorResponse newVendor = vendorService.createVendor(vendorRequest);
        return new ResponseEntity<>(newVendor, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @GetMapping("/organization/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<Page<VendorResponse>> getVendorsByOrganization(
            @PathVariable("id") Integer id,
            @ParameterObject Pageable pageable) {
        Page<VendorResponse> vendorsPage = vendorService.getVendorsByOrganization(id, pageable);
        return ResponseEntity.ok(vendorsPage);
    }
//    public ResponseEntity<List<VendorResponse>> getVendorsByOrganization(@PathVariable Integer id) {
//        List<VendorResponse> vendors = vendorService.getVendorsByOrganization(id);
//        return ResponseEntity.ok(vendors);
//    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> updateVendor(@PathVariable Long id, @Valid @RequestBody VendorRequest vendorRequest) {
        VendorResponse updatedVendor = vendorService.updateVendor(id, vendorRequest);
        return ResponseEntity.ok(updatedVendor);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok("Vendor deactivated successfully.");
    }
}