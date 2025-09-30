// service/TransactionServiceImpl.java
package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.Enum.TransactionType;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction processDebit(Organization organization, BigDecimal amount,
                                    String description, TransactionSourceType sourceType, Long sourceId) {
        if (organization.getInternalBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient funds to complete this payment.");
        }

        BigDecimal newBalance = organization.getInternalBalance().subtract(amount);
        organization.setInternalBalance(newBalance);
        organizationRepository.save(organization);

        Transaction transaction = Transaction.builder()
                .organization(organization)
                .amount(amount)
                .transactionType(TransactionType.DEBIT)
                .description(description)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .transactionDate(LocalDateTime.now())
                .balanceAfterTransaction(newBalance)
                .build();

        return transactionRepository.save(transaction);
    }

    @Override
    @Transactional
    public Transaction processCredit(Organization organization, BigDecimal amount,
                                     String description, TransactionSourceType sourceType, Long sourceId) {
        BigDecimal newBalance = organization.getInternalBalance().add(amount);
        organization.setInternalBalance(newBalance);
        organizationRepository.save(organization);

        Transaction transaction = Transaction.builder()
                .organization(organization)
                .amount(amount)
                .transactionType(TransactionType.CREDIT)
                .description(description)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .transactionDate(LocalDateTime.now())
                .balanceAfterTransaction(newBalance)
                .build();

        return transactionRepository.save(transaction);
    }
}