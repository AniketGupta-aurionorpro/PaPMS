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
import org.springframework.stereotype.Component; // Add this

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

    // In-memory sets for fast lookups
    private Set<String> existingUsernames = new HashSet<>();
    private Set<String> existingAccountNumbers = new HashSet<>();

    /**
     * This method is executed by Spring Batch BEFORE the step starts.
     * We use it to load all necessary validation data into memory one time.
     */
    @BeforeStep
    public void loadExistingData(StepExecution stepExecution) {
        log.info("Pre-loading existing usernames and account numbers for validation.");

        // Fetch all usernames from the DB in one query
        existingUsernames = appUserRepository.findAll().stream()
                .map(User::getUsername)
                .collect(Collectors.toSet());

        // Fetch all account numbers from the DB in one query
        existingAccountNumbers = bankAccountRepository.findAll().stream()
                .map(BankAccount::getAccountNumber)
                .collect(Collectors.toSet());

        log.info("Pre-loading complete. Found {} usernames and {} account numbers.", existingUsernames.size(), existingAccountNumbers.size());
    }

    @Override
    public Employee process(CsvEmployeeRecord record) throws Exception {
        // --- EFFICIENT VALIDATION LOGIC ---
        if (existingUsernames.contains(record.getUsername())) {
            log.warn("Skipping record. Username already exists: {}", record.getUsername());
            // Add the username to the set so we can also detect duplicates within the SAME file
            return null;
        }

        if (existingAccountNumbers.contains(record.getAccountNumber())) {
            log.warn("Skipping record. Bank account number already exists: {}", record.getAccountNumber());
            return null;
        }

        // Add to sets to handle duplicates within the file itself
        existingUsernames.add(record.getUsername());
        existingAccountNumbers.add(record.getAccountNumber());


        // --- TRANSFORMATION LOGIC (remains the same) ---
        // ... (rest of your process method)
        User user = User.builder()
                .username(record.getUsername())
                .password(passwordEncoder.encode(record.getPassword()))
                .fullName(record.getFullName())
                .email(record.getEmail())
                .role(Role.EMPLOYEE)
                .isActive(true)
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
//// batch/EmployeeCsvItemProcessor.java
//
//package com.aurionpro.papms.batch;
//
//import com.aurionpro.papms.Enum.OwnerType;
//import com.aurionpro.papms.Enum.Role;
//import com.aurionpro.papms.dto.CsvEmployeeRecord;
//import com.aurionpro.papms.entity.*;
//import com.aurionpro.papms.repository.AppUserRepository;
//import com.aurionpro.papms.repository.BankAccountRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//
//import java.util.List;
//
//@Slf4j
//@RequiredArgsConstructor // Use this for clean constructor injection
//public class EmployeeCsvItemProcessor implements ItemProcessor<CsvEmployeeRecord, Employee> {
//
//    // Dependencies will be final and injected by the constructor
//    private final AppUserRepository appUserRepository;
//    private final BankAccountRepository bankAccountRepository;
//    private final PasswordEncoder passwordEncoder;
//
//    @Override
//    public Employee process(CsvEmployeeRecord record) throws Exception {
//        // --- VALIDATION LOGIC ---
//        if (appUserRepository.existsByUsername(record.getUsername())) {
//            log.warn("Skipping record. Username already exists: {}", record.getUsername());
//            return null; // Returning null skips the item
//        }
//
//        if (bankAccountRepository.existsByAccountNumber(record.getAccountNumber())) {
//            log.warn("Skipping record. Bank account number already exists: {}", record.getAccountNumber());
//            return null;
//        }
//
//        // --- TRANSFORMATION LOGIC ---
//        // 1. Create User
//        User user = User.builder()
//                .username(record.getUsername())
//                .password(passwordEncoder.encode(record.getPassword()))
//                .fullName(record.getFullName())
//                .email(record.getEmail())
//                .role(Role.EMPLOYEE)
//                .isActive(true)
//                // organizationId will be set in the writer
//                .build();
//
//        // 2. Create Employee and link User
//        Employee employee = Employee.builder()
//                .user(user)
//                .employeeCode(record.getEmployeeCode())
//                .dateOfJoining(record.getDateOfJoining())
//                .department(record.getDepartment())
//                .jobTitle(record.getJobTitle())
//                .isActive(true)
//                .build();
//
//        // 3. Create Bank Account and link Employee
//        BankAccount bankAccount = BankAccount.builder()
//                .employee(employee)
//                .ownerType(OwnerType.EMPLOYEE)
//                .accountHolderName(record.getAccountHolderName())
//                .accountNumber(record.getAccountNumber())
//                .bankName(record.getBankName())
//                .ifscCode(record.getIfscCode())
//                .isPrimary(true)
//                .build();
//        employee.setBankAccount(bankAccount);
//
//        // 4. Create Salary Structure and link Employee
//        SalaryStructure salaryStructure = SalaryStructure.builder()
//                .employee(employee)
//                .basicSalary(record.getBasicSalary())
//                .hra(record.getHra())
//                .da(record.getDa())
//                .pfContribution(record.getPfContribution())
//                .otherAllowances(record.getOtherAllowances())
//                .effectiveFromDate(record.getEffectiveFromDate())
//                .isActive(true)
//                .build();
//        employee.setSalaryStructures(List.of(salaryStructure));
//
//        return employee;
//    }
//}