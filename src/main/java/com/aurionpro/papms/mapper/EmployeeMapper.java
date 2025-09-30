// mapper/EmployeeMapper.java
package com.aurionpro.papms.mapper;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;

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
}