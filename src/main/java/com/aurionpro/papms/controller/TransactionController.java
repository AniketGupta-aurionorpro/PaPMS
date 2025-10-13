package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.TransactionDto;
import com.aurionpro.papms.service.TransactionExcelReportService;
import com.aurionpro.papms.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/organizations/{organizationId}/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transaction Management", description = "APIs for viewing organization transaction history")
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionExcelReportService transactionExcelReportService; // INJECT THE NEW SERVICE

    @GetMapping
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    @Operation(summary = "Get a paginated list of all transactions for an organization",
            description = "Retrieves a list of all credit and debit transactions for a specified organization, sorted by most recent first. ORG_ADMIN can only access their own organization.")
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable) {

        log.info("Request received to get transactions for organization ID: {}", organizationId);
        Page<TransactionDto> transactions = transactionService.getTransactionsForOrganization(organizationId, pageable);
        return ResponseEntity.ok(transactions);
    }

    // ADD THIS NEW ENDPOINT
    @GetMapping("/download/excel")
    @PreAuthorize("hasAnyRole('ORG_ADMIN', 'BANK_ADMIN')")
    @Operation(summary = "Download transaction history as an Excel file",
            description = "Generates and downloads an Excel spreadsheet containing all transactions for the specified organization.")
    public ResponseEntity<byte[]> downloadTransactionReport(
            @PathVariable Integer organizationId) throws IOException {

        log.info("Request received to download Excel transaction report for organization ID: {}", organizationId);
        byte[] excelBytes = transactionExcelReportService.generateTransactionReport(organizationId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        // This header tells the browser to download the file with a specific name
        headers.setContentDispositionFormData("attachment", "transactions-org-" + organizationId + ".xlsx");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}