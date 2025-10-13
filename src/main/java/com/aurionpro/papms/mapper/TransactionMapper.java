package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.TransactionDto;
import com.aurionpro.papms.entity.Transaction;

public class TransactionMapper {

    public static TransactionDto toDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        return TransactionDto.builder()
                .id(transaction.getId())
                .transactionDate(transaction.getTransactionDate())
                .type(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .balanceAfterTransaction(transaction.getBalanceAfterTransaction())
                .sourceType(transaction.getSourceType())
                .sourceId(transaction.getSourceId())
                .build();
    }
}