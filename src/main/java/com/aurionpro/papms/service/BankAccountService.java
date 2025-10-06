package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.BankAccountResponse;
import java.util.List;

public interface BankAccountService {

    // Get all bank accounts for an organization
    List<BankAccountResponse> getBankAccountsByOrganization(Integer organizationId);

    // Get specific bank account
    BankAccountResponse getBankAccountById(Integer organizationId, Long accountId);

    // Get bank accounts for a specific employee
    List<BankAccountResponse> getEmployeeBankAccounts(Integer organizationId, Long employeeId);

    // Update bank account details
    BankAccountResponse updateBankAccount(Integer organizationId, Long accountId,
                                          BankAccountResponse request);

    // Create bank account for employee
    BankAccountResponse createEmployeeBankAccount(Integer organizationId, Long employeeId,
                                                  BankAccountResponse request);
}