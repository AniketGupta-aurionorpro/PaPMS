package com.aurionpro.papms.Enum;

/**
 * Status of a client account
 */
public enum ClientStatus {
    ACTIVE, // Client account is active and can transact
    SUSPENDED, // Temporarily suspended by admin
    INACTIVE // Permanently deactivated
}
