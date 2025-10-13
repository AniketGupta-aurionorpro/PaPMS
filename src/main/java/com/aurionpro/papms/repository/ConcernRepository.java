package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Concern;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConcernRepository extends JpaRepository<Concern, Long> {

    // For an employee to view their own concerns
    List<Concern> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    // For an ORG_ADMIN to view all concerns in their organization
    List<Concern> findByOrganizationIdOrderByCreatedAtDesc(Integer organizationId);

    // For security checks, to ensure an admin is accessing a concern within their org
    Optional<Concern> findByIdAndOrganizationId(Long concernId, Integer organizationId);
}