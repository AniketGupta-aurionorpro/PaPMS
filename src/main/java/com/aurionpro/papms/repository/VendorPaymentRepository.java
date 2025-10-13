// repository/VendorPaymentRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.vendorEntity.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface VendorPaymentRepository extends JpaRepository<VendorPayment, Long> {
    @Query("SELECT COALESCE(SUM(vp.amount), 0) FROM VendorPayment vp WHERE vp.organization.id = :orgId AND vp.status = com.aurionpro.papms.Enum.PaymentStatus.PROCESSED AND vp.paymentDate >= :startDate")
    BigDecimal findTotalPaidSince(@Param("orgId") Integer orgId, @Param("startDate") LocalDate startDate);
}