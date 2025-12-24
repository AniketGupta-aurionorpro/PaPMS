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
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService {
    private final OrganizationRepository organizationRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;

    // ... processDebit and processCredit methods remain unchanged ...

    @Override
    @Transactional(readOnly = true)
    public Page<TransactionDto> getTransactionsForOrganization(Integer organizationId, String searchTerm,
                                                               LocalDate startDate, LocalDate endDate,
                                                               TransactionType type, TransactionSourceType sourceType, Pageable pageable) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            log.warn("SECURITY ALERT: User {} (ORG_ADMIN) attempted to access transactions for organization {}",
                    currentUser.getUsername(), organizationId);
            throw new SecurityException("You are not authorized to view transactions for this organization.");
        }

        log.info("Fetching transactions for org ID {} with filters - Search: '{}', Type: {}, SourceType: {}, Start: {}, End: {}",
                organizationId, searchTerm, type, sourceType, startDate, endDate);

        Specification<Transaction> spec = (root, query, criteriaBuilder) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("organization", JoinType.LEFT);
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), organizationId));

            if (searchTerm != null && !searchTerm.isBlank()) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), "%" + searchTerm.toLowerCase() + "%"));
            }
            if (type != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), type));
            }

            // FIX: ADD THIS BLOCK TO FILTER BY SOURCE TYPE
            if (sourceType != null) {
                predicates.add(criteriaBuilder.equal(root.get("sourceType"), sourceType));
            }
            // END FIX

            if (startDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("transactionDate"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("transactionDate"), endDate.atTime(23, 59, 59)));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        Page<Transaction> transactionPage = transactionRepository.findAll(spec, pageable);
        return transactionPage.map(TransactionMapper::toDto);
    }

    // ... other helper methods remain unchanged ...
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