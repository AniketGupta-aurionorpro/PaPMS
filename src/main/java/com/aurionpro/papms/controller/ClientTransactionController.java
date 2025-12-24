package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.ClientTransactionDto;
import com.aurionpro.papms.security.CustomUserDetails;
import com.aurionpro.papms.service.ClientTransactionService;
import com.aurionpro.papms.service.client.ClientPortalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Client Transaction History
 */
@RestController
@RequestMapping("/api/client-transactions")
@RequiredArgsConstructor
@Slf4j
public class ClientTransactionController {

    private final ClientTransactionService clientTransactionService;
    private final ClientPortalService clientPortalService;

    /**
     * Get transaction history for logged-in client
     * GET /api/client-transactions/my-transactions
     */
    @GetMapping("/my-transactions")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<ClientTransactionDto.TransactionHistoryResponse> getMyTransactions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        log.info("Client {} requesting transaction history", userDetails.getUsername());
        Long clientId = clientPortalService.getClientIdByUserId(userDetails.getId());

        ClientTransactionDto.TransactionHistoryResponse response = clientTransactionService
                .getClientTransactionHistory(clientId);

        return ResponseEntity.ok(response);
    }
}
