package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    List<VendorBill> findByOrganizationId(Integer organizationId);
    Optional<VendorBill> findByIdAndOrganizationId(Long id, Integer organizationId);
}