// java/com/aurionpro/papms/batch/EmployeeCsvItemProcessor.java
package com.aurionpro.papms.batch;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.BankAccountRepository;
import com.aurionpro.papms.dto.FailedEmployeeRecord;
import org.springframework.batch.item.ExecutionContext;
import java.util.*;
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

    private List<FailedEmployeeRecord> failedRecords;
    private ExecutionContext stepExecutionContext;
    private long recordNumber = 0;

//    @BeforeStep
//    public void loadExistingData(StepExecution stepExecution) {
//        log.info("Pre-loading existing usernames and account numbers for validation.");
//
//        // Fetch all usernames from the DB in one query
//        Set<String> dbUsernames = appUserRepository.findAll().stream()
//                .map(User::getUsername)
//                .collect(Collectors.toSet());
//        existingUsernames.addAll(dbUsernames);
//
//        // Fetch all account numbers from the DB in one query
//        Set<String> dbAccountNumbers = bankAccountRepository.findAll().stream()
//                .map(BankAccount::getAccountNumber)
//                .collect(Collectors.toSet());
//        existingAccountNumbers.addAll(dbAccountNumbers);
//
//        log.info("Pre-loading complete. Found {} usernames and {} account numbers.", existingUsernames.size(), existingAccountNumbers.size());
//    }

    @BeforeStep
    public void loadExistingData(StepExecution stepExecution) {
        log.info("Pre-loading existing usernames and account numbers for validation.");

        // --- Store the execution context to add failed records to it ---
        this.stepExecutionContext = stepExecution.getExecutionContext();
        this.failedRecords = new ArrayList<>();
        this.stepExecutionContext.put("failedRecords", this.failedRecords);
        this.recordNumber = 0; // Reset for each step execution

        // ... existing data loading logic remains the same ...
        Set<String> dbUsernames = appUserRepository.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());
        existingUsernames.addAll(dbUsernames);

        Set<String> dbAccountNumbers = bankAccountRepository.findAll().stream()
                .map(BankAccount::getAccountNumber)
                .collect(Collectors.toSet());
        existingAccountNumbers.addAll(dbAccountNumbers);

        log.info("Pre-loading complete. Found {} usernames and {} account numbers.", existingUsernames.size(), existingAccountNumbers.size());
    }

    @Override
    public Employee process(CsvEmployeeRecord record) throws Exception {
        recordNumber++;

        String errorMessage = null;
        if (!existingUsernames.add(record.getUsername())) {
            errorMessage = "Username '" + record.getUsername() + "' already exists in the system or this file.";
        } else if (!existingAccountNumbers.add(record.getAccountNumber())) {
            errorMessage = "Bank account number '" + record.getAccountNumber() + "' is already in use.";
        }

        if (errorMessage != null) {
            log.warn("Skipping record #{}. Reason: {}", recordNumber, errorMessage);

            // Convert CsvEmployeeRecord to a Map for storage
            Map<String, String> rowData = new HashMap<>();
            rowData.put("username", record.getUsername());
            rowData.put("fullName", record.getFullName());
            rowData.put("email", record.getEmail());
            rowData.put("employeeCode", record.getEmployeeCode());

            // Add the failure to our list
            failedRecords.add(new FailedEmployeeRecord(recordNumber, rowData, errorMessage));

            // IMPORTANT: Update the list in the execution context
            stepExecutionContext.put("failedRecords", failedRecords);

            return null; // Skip this item
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