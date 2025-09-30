package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    Long countUserByRoleEquals(Role role);
    List<User> findByOrganizationId(Integer organizationId);

    @Query("SELECT u FROM User u WHERE u.organizationId = :organizationId AND u.role = :role")
    List<User> findByOrganizationIdAndRole(@Param("organizationId") Integer organizationId,
                                           @Param("role") Role role);
}