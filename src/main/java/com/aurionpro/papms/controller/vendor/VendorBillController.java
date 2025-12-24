package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.BillPaymentRequest;
import com.aurionpro.papms.dto.vendorDto.CreateBillRequest;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.service.vendor.BillService;
import com.aurionpro.papms.service.vendor.VendorBillPdfService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills/vendors")
@RequiredArgsConstructor
@Slf4j
public class VendorBillController {

    private final BillService billService;
    private final VendorBillPdfService vendorBillPdfService;

    // === ORG ADMIN ENDPOINTS ===

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get all vendor bills for your organization")
    public ResponseEntity<List<VendorBillDto>> getAllBills() {
        log.info("Request to get all vendor bills for current organization.");
        return ResponseEntity.ok(billService.getAllBillsForOrganization());
    }

    @GetMapping("/organization")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get all vendor bills for organization (alias)")
    public ResponseEntity<List<VendorBillDto>> getBillsForOrganization() {
        log.info("Request to get all vendor bills for current organization.");
        return ResponseEntity.ok(billService.getAllBillsForOrganization());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('VENDOR')")
    @Operation(summary = "Get a specific vendor bill by its ID")
    public ResponseEntity<VendorBillDto> getBillById(@PathVariable("id") Long billId) {
        log.info("Request to get vendor bill by ID: {}", billId);
        return ResponseEntity.ok(billService.getBillById(billId));
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Pay a vendor bill (Full/Partial/Pay Later)")
    public ResponseEntity<VendorBillDto> payBill(
            @PathVariable("id") Long billId,
            @Valid @RequestBody BillPaymentRequest request) {
        log.info("Request to pay vendor bill ID: {} with mode: {}", billId, request.getPaymentMode());
        request.setBillId(billId);
        return ResponseEntity.ok(billService.payBill(request));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ORG_ADMIN') or hasRole('VENDOR')")
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

    // === VENDOR ENDPOINTS ===

    @GetMapping("/my-bills")
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Get all bills created by logged-in vendor")
    public ResponseEntity<List<VendorBillDto>> getMyBills() {
        log.info("Request to get all bills for logged-in vendor.");
        return ResponseEntity.ok(billService.getAllBillsForVendor());
    }

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    @Operation(summary = "Create a new bill (Vendor only)")
    public ResponseEntity<VendorBillDto> createBill(@Valid @RequestBody CreateBillRequest request) {
        log.info("Request to create new vendor bill with amount: {}", request.getAmount());
        return ResponseEntity.status(HttpStatus.CREATED).body(billService.createBill(request));
    }
}