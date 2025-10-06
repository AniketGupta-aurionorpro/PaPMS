// batch/EmployeeCsvItemProcessor.java

package com.aurionpro.papms.batch;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.CsvEmployeeRecord;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.BankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@RequiredArgsConstructor // Use this for clean constructor injection
public class EmployeeCsvItemProcessor implements ItemProcessor<CsvEmployeeRecord, Employee> {

    // Dependencies will be final and injected by the constructor
    private final AppUserRepository appUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Employee process(CsvEmployeeRecord record) throws Exception {
        // --- VALIDATION LOGIC ---
        if (appUserRepository.existsByUsername(record.getUsername())) {
            log.warn("Skipping record. Username already exists: {}", record.getUsername());
            return null; // Returning null skips the item
        }

        if (bankAccountRepository.existsByAccountNumber(record.getAccountNumber())) {
            log.warn("Skipping record. Bank account number already exists: {}", record.getAccountNumber());
            return null;
        }

        // --- TRANSFORMATION LOGIC ---
        // 1. Create User
        User user = User.builder()
                .username(record.getUsername())
                .password(passwordEncoder.encode(record.getPassword()))
                .fullName(record.getFullName())
                .email(record.getEmail())
                .role(Role.EMPLOYEE)
                .isActive(true)
                // organizationId will be set in the writer
                .build();

        // 2. Create Employee and link User
        Employee employee = Employee.builder()
                .user(user)
                .employeeCode(record.getEmployeeCode())
                .dateOfJoining(record.getDateOfJoining())
                .department(record.getDepartment())
                .jobTitle(record.getJobTitle())
                .isActive(true)
                .build();

        // 3. Create Bank Account and link Employee
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

        // 4. Create Salary Structure and link Employee
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
//package com.aurionpro.papms.batch;
//
//import com.aurionpro.papms.Enum.OwnerType;
//import com.aurionpro.papms.Enum.Role;
//import com.aurionpro.papms.dto.CsvEmployeeRecord;
//import com.aurionpro.papms.entity.*;
//import com.aurionpro.papms.repository.AppUserRepository;
//import com.aurionpro.papms.repository.BankAccountRepository;
//import com.aurionpro.papms.repository.EmployeeRepository;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//// Remove @Component annotation since we're creating the bean manually
//@Slf4j
//public class EmployeeCsvItemProcessor implements ItemProcessor<CsvEmployeeRecord, Employee> {
//
//    private AppUserRepository appUserRepository;
//    private EmployeeRepository employeeRepository;
//    private BankAccountRepository bankAccountRepository;
//    private PasswordEncoder passwordEncoder;
//
//    // Setter methods for dependency injection
//    public void setAppUserRepository(AppUserRepository appUserRepository) {
//        this.appUserRepository = appUserRepository;
//    }
//
//    public void setEmployeeRepository(EmployeeRepository employeeRepository) {
//        this.employeeRepository = employeeRepository;
//    }
//
//    public void setBankAccountRepository(BankAccountRepository bankAccountRepository) {
//        this.bankAccountRepository = bankAccountRepository;
//    }
//
//    public void setPasswordEncoder(PasswordEncoder passwordEncoder) {
//        this.passwordEncoder = passwordEncoder;
//    }
//
//    @Override
//    public Employee process(CsvEmployeeRecord record) throws Exception {
//        // --- VALIDATION LOGIC ---
//        if (appUserRepository.existsByUsername(record.getUsername())) {
//            log.warn("Skipping record. Username already exists: {}", record.getUsername());
//            return null;
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
//package com.aurionpro.papms.batch;
//
//import com.aurionpro.papms.Enum.OwnerType;
//import com.aurionpro.papms.Enum.Role;
//import com.aurionpro.papms.dto.CsvEmployeeRecord;
//import com.aurionpro.papms.entity.*;
//import com.aurionpro.papms.repository.AppUserRepository;
//import com.aurionpro.papms.repository.BankAccountRepository;
//import com.aurionpro.papms.repository.EmployeeRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.batch.item.ItemProcessor;
//import org.springframework.security.crypto.password.PasswordEncoder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class EmployeeCsvItemProcessor implements ItemProcessor<CsvEmployeeRecord, Employee> {
//
//    private final AppUserRepository appUserRepository;
//    private final EmployeeRepository employeeRepository;
//    private final BankAccountRepository bankAccountRepository;
//    private final PasswordEncoder passwordEncoder;
//
//
//
//    @Override
//    public Employee process(CsvEmployeeRecord record) throws Exception {
//        // --- VALIDATION LOGIC ---
//        // Returning null from a processor tells Spring Batch to skip this item.
//        if (appUserRepository.existsByUsername(record.getUsername())) {
//            log.warn("Skipping record. Username already exists: {}", record.getUsername());
//            return null;
//        }
//        // Assuming organizationId is passed via JobExecutionContext or is a constant for the job
//        // For simplicity, we'll assume validation against a specific org is handled elsewhere or not needed here.
//        // In a real app, you'd get organizationId from JobParameters.
//        // if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, record.getEmployeeCode())) { ... }
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
//                // organizationId will be set in the writer after the org is fetched.
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
//        // Return the fully constructed Employee aggregate. It will be saved by the ItemWriter.
//        return employee;
//    }
//}