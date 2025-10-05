package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByUserId(Long userId);
    //List<Employee> findByOrganizationId(Integer organizationId);

    //paginaon ke leye dekhte he
    Page<Employee> findByOrganizationId(Integer organizationId, Pageable pageable);
    boolean existsByOrganizationIdAndEmployeeCode(Integer organizationId, String employeeCode);

    @Query("SELECT e FROM Employee e WHERE e.organization.id = :organizationId AND e.user.username = :username")
    Optional<Employee> findByOrganizationIdAndUsername(@Param("organizationId") Integer organizationId,
                                                       @Param("username") String username);
}