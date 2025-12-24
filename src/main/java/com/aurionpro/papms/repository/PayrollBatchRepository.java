package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.PayrollStatus;
import com.aurionpro.papms.entity.PayrollBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface PayrollBatchRepository
                extends JpaRepository<PayrollBatch, Long>, JpaSpecificationExecutor<PayrollBatch> {

        boolean existsByOrganizationIdAndPayrollMonthAndPayrollYear(Integer organizationId, int month, int year);

        Page<PayrollBatch> findByOrganizationId(Integer organizationId, Pageable pageable);

        // FIX: Create a new query that eagerly fetches related entities to prevent
        // LazyInitializationException
        @Query(value = "SELECT pb FROM PayrollBatch pb JOIN FETCH pb.organization JOIN FETCH pb.submittedByUser WHERE pb.status = :status", countQuery = "SELECT count(pb) FROM PayrollBatch pb WHERE pb.status = :status")
        Page<PayrollBatch> findByStatusWithDetails(@Param("status") PayrollStatus status, Pageable pageable);

        Page<PayrollBatch> findByStatus(PayrollStatus status, Pageable pageable);

        @Query("SELECT pb FROM PayrollBatch pb JOIN FETCH pb.organization JOIN FETCH pb.submittedByUser WHERE pb.id = :id")
        Optional<PayrollBatch> findByIdWithDetails(Long id);

        @Query("SELECT p.organization.id as organizationId, COUNT(p.id) as pendingCount " +
                        "FROM PayrollBatch p " +
                        "WHERE p.status = com.aurionpro.papms.Enum.PayrollStatus.PENDING_APPROVAL " +
                        "GROUP BY p.organization.id")
        List<Map<String, Object>> countPendingPayrollsByOrganization();

        List<PayrollBatch> findByOrganizationIdAndPayrollYear(Integer organizationId, int year);

        // NEW: Monthly payroll totals for dashboard chart
        @Query("SELECT new map(pb.payrollYear as year, pb.payrollMonth as month, SUM(pb.totalAmount) as totalAmount) " +
                        "FROM PayrollBatch pb " +
                        "WHERE pb.status = com.aurionpro.papms.Enum.PayrollStatus.APPROVED " +
                        "AND pb.createdAt >= :startDate " +
                        "GROUP BY pb.payrollYear, pb.payrollMonth " +
                        "ORDER BY pb.payrollYear, pb.payrollMonth")
        List<Map<String, Object>> getMonthlyPayrollTotalsSince(@Param("startDate") LocalDateTime startDate);

}