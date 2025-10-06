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
    Optional<VendorBill> findByIdAndOrganizationId(Long id, Integer organizationId);


//    SELECT vb FROM VendorBill vb: Select the VendorBill entity.
//    JOIN FETCH vb.organization: This is the key part. It tells Hibernate to fetch the organization associated with the bill in the same query.
//    JOIN FETCH vb.vendor: We also fetch the vendor at the same time, as we need vendorName for the PDF.
//    WHERE vb.id = :id AND vb.organization.id = :organizationId: The standard conditions to find the specific bill.
    @Query("SELECT vb FROM VendorBill vb JOIN FETCH vb.organization JOIN FETCH vb.vendor WHERE vb.id = :id AND vb.organization.id = :organizationId")
    Optional<VendorBill> findByIdAndOrganizationIdWithDetails(@Param("id") Long id, @Param("organizationId") Integer organizationId);
}