package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    /**
     * Finds all clients associated with a specific organization.
     *
     * @param organizationId The ID of the organization.
     * @return A list of clients for the given organization.
     */
    List<Client> findByOrganizationId(Integer organizationId);

    /**
     * Finds a client by their associated user ID.
     *
     * @param userId The ID of the user.
     * @return An Optional containing the client if found.
     */
    Optional<Client> findByUserId(Long userId);

    /**
     * Checks if a client with the given company name already exists for a specific organization.
     *
     * @param companyName    The company name to check.
     * @param organizationId The ID of the organization.
     * @return true if a client with that name exists for the organization, false otherwise.
     */
    boolean existsByCompanyNameAndOrganizationId(String companyName, Integer organizationId);
}