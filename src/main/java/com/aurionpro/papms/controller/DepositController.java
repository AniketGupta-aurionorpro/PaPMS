package com.aurionpro.papms.controller;



import com.aurionpro.papms.dto.deposit.DepositRequest;

import com.aurionpro.papms.dto.deposit.DepositResponse;
import com.aurionpro.papms.service.Deposit.DepositService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/deposits") // A new, clean base URL for deposit-related actions
@RequiredArgsConstructor
@Slf4j
public class DepositController {

    private final DepositService depositService;


    @PostMapping("/self")
    @Operation(summary = "Deposit funds into own organization's account", description = "Allows an ORG_ADMIN to increase their organization's internal balance. The organization is identified from the JWT token.")
    @PreAuthorize("hasRole('ORG_ADMIN')")
    public ResponseEntity<DepositResponse> makeSelfDeposit(
            @Valid @RequestBody DepositRequest depositRequest) {
        log.info("Received self-deposit request for amount: {}", depositRequest.getAmount());
        DepositResponse depositResponse = depositService.makeDepositForCurrentUser(depositRequest);

        log.info("Successfully processed self-deposit for organization ID: {}. New balance: {}",
                depositResponse.getOrganizationId(), depositResponse.getBalanceAfterDeposit());
        return ResponseEntity.ok(depositResponse);
    }
}