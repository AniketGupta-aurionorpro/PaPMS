package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.VendorPaymentRequest;
import com.aurionpro.papms.service.VendorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/vendors")
@RequiredArgsConstructor
public class VendorPaymentController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<String> makePaymentToVendor(@Valid @RequestBody VendorPaymentRequest request) {
        vendorService.processVendorPayment(request);
        return ResponseEntity.ok("Payment to vendor processed successfully.");
    }
}