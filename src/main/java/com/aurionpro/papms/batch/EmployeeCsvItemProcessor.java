// java/com/aurionpro/papms/batch/EmployeeCsvItemProcessor.java
package com.aurionpro.papms.batch;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class EmployeeCsvItemProcessor implements ItemProcessor<CsvEmployeeRecord, Employee> {

    private final AppUserRepository appUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;

    // In-memory sets for fast lookups. Use synchronized sets for multi-threading.
    private Set<String> existingUsernames = Collections.synchronizedSet(new HashSet<>());
    private Set<String> existingAccountNumbers = Collections.synchronizedSet(new HashSet<>());

    /**
     * This method is executed by Spring Batch BEFORE the step starts.
     * We use it to load all necessary validation data into memory one time.
     */
    @BeforeStep
    public void loadExistingData(StepExecution stepExecution) {
        log.info("Pre-loading existing usernames and account numbers for validation.");

        // Fetch all usernames from the DB in one query
        Set<String> dbUsernames = appUserRepository.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        existingUsernames.addAll(dbUsernames);

        // Fetch all account numbers from the DB in one query
        Set<String> dbAccountNumbers = bankAccountRepository.findAll().stream()
                .map(BankAccount::getAccountNumber)
                .collect(Collectors.toSet());
        existingAccountNumbers.addAll(dbAccountNumbers);

        log.info("Pre-loading complete. Found {} usernames and {} account numbers.", existingUsernames.size(), existingAccountNumbers.size());
    }

    @Override
    public Employee process(CsvEmployeeRecord record) throws Exception {
        // --- EFFICIENT VALIDATION LOGIC ---
        // The add() method returns false if the element is already in the set.
        // This atomically checks for duplicates in the DB *and* within the file itself.
        if (!existingUsernames.add(record.getUsername())) {
            log.warn("Skipping record. Username already exists: {}", record.getUsername());
            return null; // Returning null skips writing this item
        }

        if (!existingAccountNumbers.add(record.getAccountNumber())) {
            log.warn("Skipping record. Bank account number already exists: {}", record.getAccountNumber());
            return null;
        }

        // --- TRANSFORMATION LOGIC ---
        User user = User.builder()
                .username(record.getUsername())
                .password(passwordEncoder.encode(record.getPassword()))
                .fullName(record.getFullName())
                .email(record.getEmail())
                .role(Role.EMPLOYEE)
                .isActive(true)
                .requiresPasswordChange(true)
                .build();

        Employee employee = Employee.builder()
                .user(user)
                .employeeCode(record.getEmployeeCode())
                .dateOfJoining(record.getDateOfJoining())
                .department(record.getDepartment())
                .jobTitle(record.getJobTitle())
                .isActive(true)
                .build();

        BankAccount bankAccount = BankAccount.builder()
                .employee(employee)
                .ownerType(OwnerType.EMPLOYEE)
                .accountHolderName(record.getAccountHolderName())
                .accountNumber(record.getAccountNumber())
                .bankName(record.getBankName())
                .ifscCode(record.getIfscCode())
                .isPrimary(true)
                .build();
        employee.setBankAccount(bankAccount);

        SalaryStructure salaryStructure = SalaryStructure.builder()
                .employee(employee)
                .basicSalary(record.getBasicSalary())
                .hra(record.getHra())
                .da(record.getDa())
                .pfContribution(record.getPfContribution())
                .otherAllowances(record.getOtherAllowances())
                .effectiveFromDate(record.getEffectiveFromDate())
                .isActive(true)
                .build();
        employee.setSalaryStructures(List.of(salaryStructure));

        return employee;
    }
}