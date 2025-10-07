// mapper/EmployeeMapper.java
package com.aurionpro.papms.mapper;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.CompleteEmployeeRequest;
import com.aurionpro.papms.dto.CompleteEmployeeResponse;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import com.aurionpro.papms.entity.*;

public class EmployeeMapper {
    public static User toUserEntity(AddEmployeeRequest request, Integer organizationId) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.EMPLOYEE)
                .organizationId(organizationId)
                .isActive(true)
                .build();
    }

    public static Employee toEmployeeEntity(AddEmployeeRequest request, User user, Organization organization) {
        return Employee.builder()
                .user(user)
                .organization(organization)
                .employeeCode(request.getEmployeeCode())
                .dateOfJoining(request.getDateOfJoining())
                .department(request.getDepartment())
                .jobTitle(request.getJobTitle())
                .isActive(true)
                .build();
    }

    public static EmployeeResponseDto toDto(Employee employee) {
        User user = employee.getUser();
        Organization organization = employee.getOrganization();

        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(employee.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setUserEnabled(user.getIsActive());
        dto.setOrganizationId(organization.getId());
        dto.setOrganizationName(organization.getCompanyName());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setDateOfJoining(employee.getDateOfJoining());
        dto.setDepartment(employee.getDepartment());
        dto.setJobTitle(employee.getJobTitle());
        dto.setEmployeeActive(employee.getIsActive());
        return dto;
    }

    public static User toUserEntity(CompleteEmployeeRequest request, Integer organizationId) {
        return User.builder()
                .username(request.getUsername())
                .password(request.getPassword()) // Will be encoded in service
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.EMPLOYEE)
                .organizationId(organizationId)
                .isActive(true)
                .build();
    }

    // New method to create Employee entity from CompleteEmployeeRequest
    public static Employee toEmployeeEntity(CompleteEmployeeRequest request, User user, Organization organization) {
        return Employee.builder()
                .user(user)
                .organization(organization)
                .employeeCode(request.getEmployeeCode())
                .dateOfJoining(request.getDateOfJoining())
                .department(request.getDepartment())
                .jobTitle(request.getJobTitle())
                .isActive(true)
                .build();
    }

    // New method to create BankAccount entity from CompleteEmployeeRequest
    public static BankAccount toBankAccountEntity(CompleteEmployeeRequest request, Employee employee) {
        CompleteEmployeeRequest.BankAccountRequest bankAccRequest = request.getBankAccount();
        return BankAccount.builder()
                .employee(employee)
                .ownerType(OwnerType.EMPLOYEE)
                .accountHolderName(bankAccRequest.getAccountHolderName())
                .accountNumber(bankAccRequest.getAccountNumber())
                .bankName(bankAccRequest.getBankName())
                .ifscCode(bankAccRequest.getIfscCode())
                .isPrimary(true)
                .build();
        // No status field - account is automatically active
    }

    // New method to create SalaryStructure entity from CompleteEmployeeRequest
    public static SalaryStructure toSalaryStructureEntity(CompleteEmployeeRequest request, Employee employee) {
        CompleteEmployeeRequest.SalaryStructureRequest salaryRequest = request.getSalaryStructure();
        return SalaryStructure.builder()
                .employee(employee)
                .basicSalary(salaryRequest.getBasicSalary())
                .hra(salaryRequest.getHra())
                .da(salaryRequest.getDa())
                .pfContribution(salaryRequest.getPfContribution())
                .otherAllowances(salaryRequest.getOtherAllowances())
                .effectiveFromDate(salaryRequest.getEffectiveFromDate())
                .isActive(true)
                .build();
    }

    // New method to convert to CompleteEmployeeResponse
    public static CompleteEmployeeResponse toCompleteDto(Employee employee) {
        User user = employee.getUser();
        Organization organization = employee.getOrganization();

        CompleteEmployeeResponse dto = new CompleteEmployeeResponse();
        dto.setId(employee.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole().name());
        dto.setUserEnabled(user.getIsActive());
        dto.setOrganizationId(organization.getId());
        dto.setOrganizationName(organization.getCompanyName());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setDateOfJoining(employee.getDateOfJoining());
        dto.setDepartment(employee.getDepartment());
        dto.setJobTitle(employee.getJobTitle());
        dto.setEmployeeActive(employee.getIsActive());
        dto.setCreatedAt(employee.getCreatedAt());

        // Map bank account
        if (employee.getBankAccount() != null) {
            CompleteEmployeeResponse.BankAccountResponse bankAccountDto = new CompleteEmployeeResponse.BankAccountResponse();
            bankAccountDto.setId(Math.toIntExact(employee.getBankAccount().getId()));
            bankAccountDto.setAccountHolderName(employee.getBankAccount().getAccountHolderName());
            bankAccountDto.setAccountNumber(employee.getBankAccount().getAccountNumber());
            bankAccountDto.setBankName(employee.getBankAccount().getBankName());
            bankAccountDto.setIfscCode(employee.getBankAccount().getIfscCode());
            bankAccountDto.setPrimary(employee.getBankAccount().isPrimary());
            dto.setBankAccount(bankAccountDto);
        }

        // Map current salary structure
        SalaryStructure currentSalary = employee.getCurrentSalaryStructure();
        if (currentSalary != null) {
            CompleteEmployeeResponse.SalaryStructureResponse salaryDto = new CompleteEmployeeResponse.SalaryStructureResponse();
            salaryDto.setId(currentSalary.getId());
            salaryDto.setBasicSalary(currentSalary.getBasicSalary());
            salaryDto.setHra(currentSalary.getHra());
            salaryDto.setDa(currentSalary.getDa());
            salaryDto.setPfContribution(currentSalary.getPfContribution());
            salaryDto.setOtherAllowances(currentSalary.getOtherAllowances());
            salaryDto.setTotalSalary(currentSalary.getTotalSalary());
            salaryDto.setEffectiveFromDate(currentSalary.getEffectiveFromDate());
            salaryDto.setActive(currentSalary.getIsActive());
            dto.setCurrentSalary(salaryDto);
        }

        return dto;
    }
}