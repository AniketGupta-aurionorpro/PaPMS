// repository/VendorRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.PayrollBatch;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long>, JpaSpecificationExecutor<Vendor> {

    Page<Vendor> findByOrganizationId(Integer organizationId, Pageable pageable);
    boolean existsByVendorNameAndOrganizationId(String vendorName, Integer organizationId);
    // Add this inside the VendorRepository interface
    long countByOrganizationIdAndIsActiveTrue(Integer organizationId);
}