package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.VendorBillDto;

import com.aurionpro.papms.service.vendor.BillService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
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
public class VendorBillController {

    private final BillService billService;

    @GetMapping
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get all vendor bills for your organization")
    public ResponseEntity<List<VendorBillDto>> getAllBills() {
        return ResponseEntity.ok(billService.getAllBillsForOrganization());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Get a specific vendor bill by its ID")
    public ResponseEntity<VendorBillDto> getBillById(@PathVariable("id") Long billId) {
        return ResponseEntity.ok(billService.getBillById(billId));
    }
}