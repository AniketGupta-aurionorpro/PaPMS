// repository/VendorPaymentRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.vendorEntity.VendorPayment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorPaymentRepository extends JpaRepository<VendorPayment, Long> {
}