package com.aurionpro.papms.Enum;

/**
 * Status of a client deposit request
 */
public enum ClientDepositStatus {
    PENDING, // Waiting for ORG_ADMIN approval
    APPROVED, // Approved and funds added to client balance
    REJECTED // Rejected by ORG_ADMIN
}
