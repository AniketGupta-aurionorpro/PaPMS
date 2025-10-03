// repository/VendorRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.vendorEntity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Long> {
    List<Vendor> findByOrganizationId(Integer organizationId);
    boolean existsByVendorNameAndOrganizationId(String vendorName, Integer organizationId);
}