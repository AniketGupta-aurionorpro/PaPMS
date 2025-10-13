// service/TransactionServiceImpl.java
package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.Enum.TransactionType;
import com.aurionpro.papms.dto.TransactionDto;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.mapper.TransactionMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // ADDED
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j // ADDED
public class TransactionServiceImpl implements TransactionService {
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    @Override
    @Transactional
    public Transaction processDebit(Organization organization, BigDecimal amount,
                                    String description, TransactionSourceType sourceType, Long sourceId) {
        validateOrganizationIsActive(organization);
        log.info("Processing DEBIT of {} for organization ID {} [{}]. Source: {}/{}",
                amount, organization.getId(), organization.getCompanyName(), sourceType, sourceId);
        if (organization.getInternalBalance().compareTo(amount) < 0) {
            log.error("Insufficient funds for debit. Org ID: {}, Required: {}, Available: {}",
                    organization.getId(), amount, organization.getInternalBalance());
            throw new IllegalStateException("Insufficient funds to complete this payment.");
        }
        //update the organization's balance.
        BigDecimal newBalance = organization.getInternalBalance().subtract(amount);
        organization.setInternalBalance(newBalance);
        organizationRepository.save(organization);
        log.info("Organization {} balance updated to {}. Transaction for '{}' is being recorded.", organization.getId(), newBalance, description);

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
        validateOrganizationIsActive(organization);
        log.info("Processing CREDIT of {} for organization ID {} [{}]. Source: {}/{}",
                amount, organization.getId(), organization.getCompanyName(), sourceType, sourceId);
        BigDecimal newBalance = organization.getInternalBalance().add(amount);
        organization.setInternalBalance(newBalance);
        organizationRepository.save(organization);
        log.info("Organization {} balance updated to {}. Transaction for '{}' is being recorded.", organization.getId(), newBalance, description);

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

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsForOrganization(Integer organizationId, Pageable pageable) {
        User currentUser = getLoggedInUser();

        // Security Check: ORG_ADMIN can only view their own organization's transactions.
        // BANK_ADMIN can view any.
        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            log.warn("SECURITY ALERT: User {} (ORG_ADMIN) attempted to access transactions for organization {}",
                    currentUser.getUsername(), organizationId);
            throw new SecurityException("You are not authorized to view transactions for this organization.");
        }

        log.info("Fetching transactions for organization ID {} with pagination: {}", organizationId, pageable);
        Page<Transaction> transactionPage = transactionRepository
                .findByOrganizationIdOrderByTransactionDateDesc(organizationId, pageable);

        // Convert the Page<Transaction> to Page<TransactionDto> using the mapper
        return transactionPage.map(TransactionMapper::toDto);
    }
    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    private void validateOrganizationIsActive(Organization organization) {
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            log.error("Transaction blocked for non-active organization ID: {}. Status is: {}", organization.getId(), organization.getStatus());
            throw new IllegalStateException("Transactions are not permitted for organizations with status: " + organization.getStatus());
        }
    }

}