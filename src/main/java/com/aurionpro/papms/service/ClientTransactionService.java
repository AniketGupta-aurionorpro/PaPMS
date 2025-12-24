package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.ClientTransactionDto;

/**
 * Service interface for Client Transaction operations
 */
public interface ClientTransactionService {

    /**
     * Get transaction history for a client
     * Includes deposit requests and invoice payments
     *
     * @param clientId Client ID
     * @return Transaction history response
     */
    ClientTransactionDto.TransactionHistoryResponse getClientTransactionHistory(Long clientId);
}
