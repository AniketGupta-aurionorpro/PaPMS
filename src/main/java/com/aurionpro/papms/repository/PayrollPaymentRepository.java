package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.PayrollPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page; // Import Page
import org.springframework.data.domain.Pageable;
import java.util.Optional;
import java.util.List;

@Repository
public interface PayrollPaymentRepository extends JpaRepository<PayrollPayment, Long> {

    @Query("SELECT pp FROM PayrollPayment pp JOIN FETCH pp.employee emp JOIN FETCH emp.user JOIN FETCH pp.payrollBatch pb JOIN FETCH pb.organization WHERE pp.id = :paymentId")
    Optional<PayrollPayment> findByIdWithDetails(Long paymentId);

    List<PayrollPayment> findByPayrollBatchOrganizationIdAndPayrollBatchPayrollYearAndPayrollBatchPayrollMonth(Integer organizationId, int year, int month);

    @Query(value = "SELECT pp FROM PayrollPayment pp WHERE pp.employee.id = :employeeId",
            countQuery = "SELECT count(pp) FROM PayrollPayment pp WHERE pp.employee.id = :employeeId")
    Page<PayrollPayment> findByEmployeeIdWithPagination(Long employeeId, Pageable pageable);
}