package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.ClientDepositStatus;
import com.aurionpro.papms.Enum.InvoiceStatus;
import com.aurionpro.papms.dto.ClientTransactionDto;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.ClientDepositRequest;
import com.aurionpro.papms.entity.Invoice;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.ClientDepositRequestRepository;
import com.aurionpro.papms.repository.ClientRepository;
import com.aurionpro.papms.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Implementation of ClientTransactionService
 * Aggregates deposit requests and invoice payments into transaction history
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ClientTransactionServiceImpl implements ClientTransactionService {

    private final ClientRepository clientRepository;
    private final ClientDepositRequestRepository depositRequestRepository;
    private final InvoiceRepository invoiceRepository;

    @Override
    public ClientTransactionDto.TransactionHistoryResponse getClientTransactionHistory(Long clientId) {
        log.info("Fetching transaction history for client ID: {}", clientId);

        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        List<ClientTransactionDto.TransactionRecord> transactions = new ArrayList<>();

        // Get approved deposit requests
        List<ClientDepositRequest> deposits = depositRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId);
        BigDecimal totalDeposited = BigDecimal.ZERO;

        for (ClientDepositRequest deposit : deposits) {
            String status = deposit.getStatus().name();
            LocalDateTime date = deposit.getStatus() == ClientDepositStatus.APPROVED
                    ? deposit.getProcessedAt()
                    : deposit.getCreatedAt();

            transactions.add(ClientTransactionDto.TransactionRecord.builder()
                    .id(deposit.getId())
                    .type("DEPOSIT_REQUEST")
                    .description("Wallet deposit request"
                            + (deposit.getRemarks() != null ? ": " + deposit.getRemarks() : ""))
                    .amount(deposit.getAmount())
                    .status(status)
                    .transactionDate(date)
                    .referenceNumber(deposit.getReferenceNumber())
                    .build());

            if (deposit.getStatus() == ClientDepositStatus.APPROVED) {
                totalDeposited = totalDeposited.add(deposit.getAmount());
            }
        }

        // Get invoice payments (invoices where paidAmount > 0)
        List<Invoice> invoices = invoiceRepository.findByClientId(clientId.intValue());
        BigDecimal totalSpent = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            if (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                transactions.add(ClientTransactionDto.TransactionRecord.builder()
                        .id(invoice.getId().longValue())
                        .type("INVOICE_PAYMENT")
                        .description("Invoice payment: " + invoice.getInvoiceNumber())
                        .amount(invoice.getPaidAmount().negate()) // Negative for payment
                        .status(invoice.getStatus().name())
                        .transactionDate(invoice.getPaidAt() != null ? invoice.getPaidAt() : invoice.getUpdatedAt())
                        .referenceNumber(invoice.getInvoiceNumber())
                        .build());

                totalSpent = totalSpent.add(invoice.getPaidAmount());
            }
        }

        // Sort by date descending
        transactions.sort(Comparator.comparing(ClientTransactionDto.TransactionRecord::getTransactionDate).reversed());

        log.info("Found {} transactions for client {}", transactions.size(), clientId);

        return ClientTransactionDto.TransactionHistoryResponse.builder()
                .transactions(transactions)
                .currentBalance(client.getBalance())
                .totalDeposited(totalDeposited)
                .totalSpent(totalSpent)
                .build();
    }
}
