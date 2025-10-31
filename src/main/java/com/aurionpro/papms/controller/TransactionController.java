package com.aurionpro.papms.controller;

import com.aurionpro.papms.Enum.TransactionType;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDate;

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
    @Operation(summary = "Get a paginated and filtered list of all transactions for an organization")
    public ResponseEntity<Page<TransactionDto>> getTransactions(
            @PathVariable Integer organizationId,
            @ParameterObject Pageable pageable,
            // --- NEW FILTER PARAMETERS ---
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type) {

        Page<TransactionDto> transactions = transactionService.getTransactionsForOrganization(
                organizationId, searchTerm, startDate, endDate, type, pageable);

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