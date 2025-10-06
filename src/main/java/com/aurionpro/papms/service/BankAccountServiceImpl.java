package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.BankAccountResponse;
import com.aurionpro.papms.entity.BankAccount;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.BankAccountRepository;
import com.aurionpro.papms.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final EmployeeRepository employeeRepository;
    private final AppUserRepository appUserRepository;

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getBankAccountsByOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();
        validateOrganizationAccess(currentUser, organizationId);

        List<BankAccount> accounts = bankAccountRepository.findByEmployeeOrganizationId(organizationId);

        return accounts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public BankAccountResponse getBankAccountById(Integer organizationId, Long accountId) {
        User currentUser = getLoggedInUser();
        validateOrganizationAccess(currentUser, organizationId);

        BankAccount account = bankAccountRepository.findByIdAndEmployeeOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with ID: " + accountId));

        return convertToResponse(account);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BankAccountResponse> getEmployeeBankAccounts(Integer organizationId, Long employeeId) {
        User currentUser = getLoggedInUser();
        validateOrganizationAccess(currentUser, organizationId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        if (currentUser.getRole() == Role.EMPLOYEE && !employee.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("Employees can only view their own bank accounts");
        }

        List<BankAccount> accounts = bankAccountRepository.findByEmployeeIdAndEmployeeOrganizationId(employeeId, organizationId);

        return accounts.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BankAccountResponse updateBankAccount(Integer organizationId, Long accountId, BankAccountResponse request) {
        User currentUser = getLoggedInUser();
        validateOrganizationAccess(currentUser, organizationId);

        BankAccount account = bankAccountRepository.findByIdAndEmployeeOrganizationId(accountId, organizationId)
                .orElseThrow(() -> new NotFoundException("Bank account not found with ID: " + accountId));

        // Update account details
        account.setAccountHolderName(request.getAccountHolderName());
        account.setBankName(request.getBankName());
        account.setIfscCode(request.getIfscCode());
        account.setPrimary(request.isPrimary());

        BankAccount updatedAccount = bankAccountRepository.save(account);

        log.info("Bank account {} updated by user {}", accountId, currentUser.getUsername());

        return convertToResponse(updatedAccount);
    }

    @Override
    @Transactional
    public BankAccountResponse createEmployeeBankAccount(Integer organizationId, Long employeeId, BankAccountResponse request) {
        User currentUser = getLoggedInUser();
        validateOrganizationAccess(currentUser, organizationId);

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        if (!employee.getOrganization().getId().equals(organizationId)) {
            throw new SecurityException("Employee does not belong to this organization");
        }

        BankAccount bankAccount = BankAccount.builder()
                .employee(employee)
                .ownerType(OwnerType.EMPLOYEE)
                .accountHolderName(request.getAccountHolderName())
                .accountNumber(request.getAccountNumber())
                .bankName(request.getBankName())
                .ifscCode(request.getIfscCode())
                .isPrimary(request.isPrimary())
                .build();

        BankAccount savedAccount = bankAccountRepository.save(bankAccount);

        log.info("Bank account created for employee {} by user {}", employeeId, currentUser.getUsername());

        return convertToResponse(savedAccount);
    }

    // Helper Methods
    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("Authenticated user not found"));
    }

    private void validateOrganizationAccess(User currentUser, Integer organizationId) {
        if (currentUser.getRole() == Role.EMPLOYEE || currentUser.getRole() == Role.ORG_ADMIN) {
            if (!currentUser.getOrganizationId().equals(organizationId)) {
                throw new SecurityException("Access denied to organization: " + organizationId);
            }
        }
    }

    private BankAccountResponse convertToResponse(BankAccount account) {
        String employeeName = null;
        Long employeeId = null;

        if (account.getEmployee() != null && account.getEmployee().getUser() != null) {
            employeeName = account.getEmployee().getUser().getFullName();
            employeeId = account.getEmployee().getId();
        }

        return BankAccountResponse.builder()
                .id(account.getId())
                .employeeId(employeeId)
                .employeeName(employeeName)
                .accountHolderName(account.getAccountHolderName())
                .accountNumber(maskAccountNumber(account.getAccountNumber()))
                .bankName(account.getBankName())
                .ifscCode(account.getIfscCode())
                .isPrimary(account.isPrimary())
                .build();
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber;
        }
        return "XXXXXX" + accountNumber.substring(accountNumber.length() - 4);
    }
}