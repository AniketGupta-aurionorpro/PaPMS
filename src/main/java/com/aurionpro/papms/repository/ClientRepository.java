package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    /**
     * Finds all clients associated with a specific organization.
     */
    List<Client> findByOrganizationId(Integer organizationId);

    /**
     * Finds all clients for organization with pagination.
     */
    Page<Client> findByOrganizationId(Integer organizationId, Pageable pageable);

    /**
     * Finds a client by their associated user ID.
     */
    Optional<Client> findByUserId(Long userId);

    /**
     * Checks if a client with the given client name already exists for a specific
     * organization.
     */
    boolean existsByClientNameAndOrganizationId(String clientName, Integer organizationId);

    /**
     * Checks if a client with the given contact email already exists.
     */
    boolean existsByContactEmail(String email);
}