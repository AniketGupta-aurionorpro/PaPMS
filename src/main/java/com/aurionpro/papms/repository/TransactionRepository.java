// repository/TransactionRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    // Page<Transaction> findByOrganizationIdOrderByTransactionDateDesc(Integer
    // organizationId, Pageable pageable);
    // List<Transaction> findAllByOrganizationIdOrderByTransactionDateDesc(Integer
    // organizationId);
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

    @Query("SELECT new map(YEAR(t.transactionDate) as year, MONTH(t.transactionDate) as month, sum(t.amount) as totalVolume) "
            +
            "FROM Transaction t " +
            "WHERE t.transactionDate >= :startDate " +
            "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
            "ORDER BY YEAR(t.transactionDate), MONTH(t.transactionDate) ASC")
    List<Map<String, Object>> getMonthlyTransactionVolumeSince(@Param("startDate") LocalDateTime startDate);

    // NEW: Monthly transaction count for dashboard chart (tracks system activity)
    @Query("SELECT new map(YEAR(t.transactionDate) as year, MONTH(t.transactionDate) as month, COUNT(t) as count) " +
            "FROM Transaction t " +
            "WHERE t.transactionDate >= :startDate " +
            "GROUP BY YEAR(t.transactionDate), MONTH(t.transactionDate) " +
            "ORDER BY YEAR(t.transactionDate), MONTH(t.transactionDate) ASC")
    List<Map<String, Object>> getMonthlyTransactionCountSince(@Param("startDate") LocalDateTime startDate);
}