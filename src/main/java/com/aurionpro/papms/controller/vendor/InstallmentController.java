package com.aurionpro.papms.controller.vendor;

import com.aurionpro.papms.dto.vendorDto.CreateInstallmentPlanRequest;
import com.aurionpro.papms.dto.vendorDto.InstallmentDto;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.service.vendor.InstallmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills/installments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Installments", description = "APIs for managing bill installment payments")
public class InstallmentController {

    private final InstallmentService installmentService;

    @PostMapping("/plan")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Create an installment plan for a bill")
    public ResponseEntity<VendorBillDto> createInstallmentPlan(
            @Valid @RequestBody CreateInstallmentPlanRequest request) {
        log.info("Request to create installment plan for bill ID: {} with {} installments",
                request.getBillId(), request.getNumberOfInstallments());
        VendorBillDto result = installmentService.createInstallmentPlan(request);
        log.info("Successfully created installment plan for bill {}", result.getBillNumber());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    @Operation(summary = "Pay a specific installment")
    public ResponseEntity<VendorBillDto> payInstallment(@PathVariable("id") Long installmentId) {
        log.info("Request to pay installment ID: {}", installmentId);
        VendorBillDto result = installmentService.payInstallment(installmentId);
        log.info("Successfully paid installment. Bill {} now has {} paid",
                result.getBillNumber(), result.getPaidAmount());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/bill/{billId}")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'VENDOR')")
    @Operation(summary = "Get all installments for a bill")
    public ResponseEntity<List<InstallmentDto>> getInstallmentsForBill(
            @PathVariable("billId") Long billId) {
        log.info("Request to get installments for bill ID: {}", billId);
        List<InstallmentDto> installments = installmentService.getInstallmentsForBill(billId);
        log.info("Found {} installments for bill ID {}", installments.size(), billId);
        return ResponseEntity.ok(installments);
    }
}
