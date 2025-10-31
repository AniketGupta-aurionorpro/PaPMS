package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorRequest;
import com.aurionpro.papms.dto.vendorDto.VendorResponse;
import com.aurionpro.papms.service.vendor.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class VendorController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    // convert the JSON from the request body into a VendorRequest object
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody VendorRequest vendorRequest) {
        log.info("Request to create a new vendor: {}", vendorRequest.getVendorName());
        VendorResponse newVendor = vendorService.createVendor(vendorRequest);
        log.info("Successfully created vendor '{}' with ID: {}", newVendor.getVendorName(), newVendor.getId());
        return new ResponseEntity<>(newVendor, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> getVendorById(@PathVariable Long id) {
        log.info("Request to get vendor by ID: {}", id);
        return ResponseEntity.ok(vendorService.getVendorById(id));
    }

    @GetMapping("/organization/{id}")
    @PreAuthorize("hasAnyRole('BANK_ADMIN', 'ORG_ADMIN')")
    public ResponseEntity<Page<VendorResponse>> getVendorsByOrganization(
            @PathVariable("id") Integer id,
            @ParameterObject Pageable pageable) {
        log.info("Request to get vendors for organization ID {} with pagination {}", id, pageable);
        Page<VendorResponse> vendorsPage = vendorService.getVendorsByOrganization(id, pageable);
        log.info("Returning {} vendors for organization ID {}", vendorsPage.getTotalElements(), id);
        return ResponseEntity.ok(vendorsPage);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<VendorResponse> updateVendor(@PathVariable Long id, @Valid @RequestBody VendorRequest vendorRequest) {
        log.info("Request to update vendor ID: {}", id);
        VendorResponse updatedVendor = vendorService.updateVendor(id, vendorRequest);
        log.info("Successfully updated vendor ID: {}", id);
        return ResponseEntity.ok(updatedVendor);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> deleteVendor(@PathVariable Long id) {
        log.info("Request to delete (deactivate) vendor ID: {}", id);
        vendorService.deleteVendor(id);
        log.info("Successfully deactivated vendor ID: {}", id);
        return ResponseEntity.ok("Vendor deactivated successfully.");
    }
}