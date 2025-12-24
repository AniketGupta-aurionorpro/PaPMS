package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    List<VendorBill> findByOrganizationId(Integer organizationId);

    List<VendorBill> findByVendorId(Long vendorId);

    Optional<VendorBill> findByIdAndOrganizationId(Long id, Integer organizationId);

    @Query("SELECT vb FROM VendorBill vb JOIN FETCH vb.organization JOIN FETCH vb.vendor WHERE vb.id = :id AND vb.organization.id = :organizationId")
    Optional<VendorBill> findByIdAndOrganizationIdWithDetails(@Param("id") Long id,
            @Param("organizationId") Integer organizationId);
}
