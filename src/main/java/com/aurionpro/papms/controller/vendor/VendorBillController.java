package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorBillDto;

import com.aurionpro.papms.service.vendor.BillService;
import com.aurionpro.papms.service.vendor.VendorBillPdfService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bills/vendors")
@RequiredArgsConstructor
@Slf4j
public class VendorBillController {

    private final BillService billService;
    private final VendorBillPdfService vendorBillPdfService; // naya SERVICE

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get all vendor bills for your organization")
    public ResponseEntity<List<VendorBillDto>> getAllBills() {
        log.info("Request to get all vendor bills for current organization.");
        return ResponseEntity.ok(billService.getAllBillsForOrganization());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get a specific vendor bill by its ID")
    public ResponseEntity<VendorBillDto> getBillById(@PathVariable("id") Long billId) {
        log.info("Request to get vendor bill by ID: {}", billId);
        return ResponseEntity.ok(billService.getBillById(billId));
    }

    // ADD THIS NEW ENDPOINT
    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Download a specific vendor bill as a PDF")
    public ResponseEntity<byte[]> downloadVendorBillPdf(@PathVariable("id") Long billId) {
        log.info("Request to download PDF for vendor bill ID: {}", billId);
        byte[] pdfBytes = vendorBillPdfService.generateVendorBillPdf(billId);
        log.info("Successfully generated PDF for vendor bill ID {}", billId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "vendor-bill-" + billId + ".pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}