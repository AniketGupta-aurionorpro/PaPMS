package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.VendorRequest;
import com.aurionpro.papms.dto.VendorResponse;
import com.aurionpro.papms.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    /**
     * Endpoint to create a new vendor.
     * Only users with the ORG_ADMIN role can access this.
     */
    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody VendorRequest vendorRequest) {
        VendorResponse newVendor = vendorService.createVendor(vendorRequest);
        return new ResponseEntity<>(newVendor, HttpStatus.CREATED);
    }

    /**
     * Endpoint to get a specific vendor by its ID.
     * Accessible by ORG_ADMIN. The service layer ensures they can only access their own vendors.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    /**
     * Endpoint to get all vendors for a specific organization.
     * Accessible by ORG_ADMIN.
     */
    @GetMapping("/organization/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<List<VendorResponse>> getVendorsByOrganization(@PathVariable Integer id) {
        List<VendorResponse> vendors = vendorService.getVendorsByOrganization(id);
        return ResponseEntity.ok(vendors);
    }

    /**
     * Endpoint to update an existing vendor's details.
     * Only ORG_ADMIN can update vendors.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> updateVendor(@PathVariable Long id, @Valid @RequestBody VendorRequest vendorRequest) {
        VendorResponse updatedVendor = vendorService.updateVendor(id, vendorRequest);
        return ResponseEntity.ok(updatedVendor);
    }

    /**
     * Endpoint to soft-delete (deactivate) a vendor.
     * Only ORG_ADMIN can delete vendors.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok("Vendor deactivated successfully.");
    }
}