// repository/SalaryStructureRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {

    List<SalaryStructure> findByEmployeeId(Long employeeId);

    Optional<SalaryStructure> findByEmployeeIdAndIsActiveTrue(Long employeeId);

    @Query("SELECT ss FROM SalaryStructure ss WHERE ss.employee.organization.id = :organizationId AND ss.isActive = true")
    List<SalaryStructure> findActiveByOrganizationId(@Param("organizationId") Integer organizationId);

    boolean existsByEmployeeIdAndIsActiveTrue(Long employeeId);
}