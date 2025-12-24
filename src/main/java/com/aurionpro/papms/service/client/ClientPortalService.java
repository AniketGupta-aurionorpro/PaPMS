package com.aurionpro.papms.service.client;

import com.aurionpro.papms.dto.ClientDto;
import com.aurionpro.papms.dto.OnboardClientRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for Client Portal operations
 * Handles client onboarding with auto-generated credentials
 */
public interface ClientPortalService {

    /**
     * Onboard a new client - creates user account, sends welcome email with
     * credentials
     * 
     * @param request client information
     * @return created client DTO with balance initialized to 0
     */
    ClientDto onboardClient(OnboardClientRequest request);

    /**
     * Get all clients for the logged-in organization admin with pagination
     * 
     * @param pageable pagination information
     * @return page of client DTOs
     */
    Page<ClientDto> getAllClients(Pageable pageable);

    /**
     * Get client by ID (org admin only)
     * 
     * @param clientId client ID
     * @return client DTO
     */
    ClientDto getClientById(Long clientId);

    /**
     * Get current client profile (client only)
     * 
     * @return client DTO
     */
    ClientDto getMyProfile();

    /**
     * Suspend client account
     * 
     * @param clientId client ID
     */
    void suspendClient(Long clientId);

    /**
     * Activate suspended client account
     * 
     * @param clientId client ID
     */
    void activateClient(Long clientId);

    /**
     * Get client ID by user ID
     */
    Long getClientIdByUserId(Long userId);
}
