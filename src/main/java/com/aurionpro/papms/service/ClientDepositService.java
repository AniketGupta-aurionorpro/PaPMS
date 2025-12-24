package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.ClientDepositRequestDto;

import java.util.List;

/**
 * Service interface for Client Deposit Request operations
 */
public interface ClientDepositService {

    /**
     * Create a new deposit request (called by CLIENT)
     */
    ClientDepositRequestDto.Response createDepositRequest(
            ClientDepositRequestDto.CreateRequest request, Long clientId);

    /**
     * Get all deposit requests for a client
     */
    List<ClientDepositRequestDto.Response> getClientDepositRequests(Long clientId);

    /**
     * Get all deposit requests for an organization (for ORG_ADMIN)
     */
    List<ClientDepositRequestDto.Response> getOrganizationDepositRequests(Integer organizationId);

    /**
     * Get all pending deposit requests for an organization
     */
    List<ClientDepositRequestDto.Response> getPendingDepositRequests(Integer organizationId);

    /**
     * Approve a deposit request (called by ORG_ADMIN)
     */
    ClientDepositRequestDto.Response approveDeposit(Long depositId, Integer approvedBy);

    /**
     * Reject a deposit request (called by ORG_ADMIN)
     */
    ClientDepositRequestDto.Response rejectDeposit(Long depositId, String reason, Integer rejectedBy);

    /**
     * Get deposit request by ID
     */
    ClientDepositRequestDto.Response getDepositById(Long depositId);

    /**
     * Get pending count for organization
     */
    long getPendingCount(Integer organizationId);
}
