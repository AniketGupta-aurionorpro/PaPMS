package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorPaymentRequest;
import com.aurionpro.papms.service.vendor.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/vendors") // The endpoint to send payments
@RequiredArgsConstructor
public class VendorPaymentController {

    private final VendorService vendorService;

    @PostMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create and process a payment to a vendor",
            description = "Creates a payment record, immediately debits the organization's balance, and sends a notification.")
    public ResponseEntity<String> makePaymentToVendor(@Valid @RequestBody VendorPaymentRequest request) {

        vendorService.processVendorPayment(request);
        return ResponseEntity.ok("Payment to vendor processed and sent successfully.");
    }
}