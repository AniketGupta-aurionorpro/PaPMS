package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.ClientDepositStatus;
import com.aurionpro.papms.entity.ClientDepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for ClientDepositRequest entity
 */
@Repository
public interface ClientDepositRequestRepository extends JpaRepository<ClientDepositRequest, Long> {

    /**
     * Find all deposit requests by client
     */
    List<ClientDepositRequest> findByClientIdOrderByCreatedAtDesc(Long clientId);

    /**
     * Find all deposit requests by organization
     */
    List<ClientDepositRequest> findByOrganizationIdOrderByCreatedAtDesc(Integer organizationId);

    /**
     * Find all pending deposit requests by organization
     */
    List<ClientDepositRequest> findByOrganizationIdAndStatusOrderByCreatedAtDesc(
            Integer organizationId, ClientDepositStatus status);

    /**
     * Find pending deposit requests count for an organization
     */
    @Query("SELECT COUNT(d) FROM ClientDepositRequest d WHERE d.organization.id = :orgId AND d.status = :status")
    long countByOrganizationIdAndStatus(@Param("orgId") Integer orgId, @Param("status") ClientDepositStatus status);

    /**
     * Find deposit requests by client and status
     */
    List<ClientDepositRequest> findByClientIdAndStatusOrderByCreatedAtDesc(Long clientId, ClientDepositStatus status);
}
