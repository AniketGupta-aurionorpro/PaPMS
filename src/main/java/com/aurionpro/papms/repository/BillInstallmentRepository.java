package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.InstallmentStatus;
import com.aurionpro.papms.entity.vendorEntity.BillInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillInstallmentRepository extends JpaRepository<BillInstallment, Long> {

    // Get all installments for a bill ordered by number
    List<BillInstallment> findByBillIdOrderByInstallmentNumberAsc(Long billId);

    // Find installments that are pending and past due date (for overdue marking)
    List<BillInstallment> findByStatusAndDueDateBefore(InstallmentStatus status, LocalDate date);

    // Find next pending installment for a bill
    Optional<BillInstallment> findFirstByBillIdAndStatusOrderByInstallmentNumberAsc(Long billId,
            InstallmentStatus status);

    // Count paid installments for a bill
    @Query("SELECT COUNT(i) FROM BillInstallment i WHERE i.bill.id = :billId AND i.status = 'PAID'")
    Long countPaidInstallments(@Param("billId") Long billId);

    // Check if all installments are paid
    @Query("SELECT CASE WHEN COUNT(i) = 0 THEN true ELSE false END FROM BillInstallment i " +
            "WHERE i.bill.id = :billId AND i.status != 'PAID'")
    boolean areAllInstallmentsPaid(@Param("billId") Long billId);
}
