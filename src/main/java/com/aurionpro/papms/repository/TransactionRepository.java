// repository/TransactionRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByOrganizationIdOrderByTransactionDateDesc(Integer organizationId, Pageable pageable);
    List<Transaction> findAllByOrganizationIdOrderByTransactionDateDesc(Integer organizationId);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.organization.id = :orgId AND t.transactionDate >= :startDate")
    BigDecimal findTotalVolumeSince(@Param("orgId") Integer orgId, @Param("startDate") LocalDateTime startDate);

    long countByOrganizationIdAndTransactionDateAfter(Integer organizationId, LocalDateTime afterDate);
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.organization.id = :orgId AND t.transactionType = com.aurionpro.papms.Enum.TransactionType.CREDIT")
    BigDecimal findTotalCreditsByOrganizationId(@Param("orgId") Integer orgId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.organization.id = :orgId AND t.transactionType = com.aurionpro.papms.Enum.TransactionType.DEBIT")
    BigDecimal findTotalDebitsByOrganizationId(@Param("orgId") Integer orgId);

    long countByOrganizationId(Integer organizationId);

    Optional<Transaction> findFirstByOrganizationIdOrderByTransactionDateAsc(Integer organizationId);

    Optional<Transaction> findFirstByOrganizationIdOrderByTransactionDateDesc(Integer organizationId);
}