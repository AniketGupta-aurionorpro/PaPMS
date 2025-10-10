package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.BulkEmployeeUploadResponse;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aurionpro.papms.dto.CompleteEmployeeResponse;
import java.util.List;

public interface EmployeeService {
    void addEmployee(Integer organizationId, AddEmployeeRequest request);
    BulkEmployeeUploadResponse bulkAddEmployees(Integer organizationId, MultipartFile file);
    //List<EmployeeResponseDto> getEmployeesByOrganization(Integer organizationId);
    // MODIFIED: Updated the method signature for pagination
    Page<EmployeeResponseDto> getEmployeesByOrganization(Integer organizationId, Pageable pageable);
    EmployeeResponseDto getEmployeeById(Integer organizationId, Long employeeId);
    void deleteEmployee(Integer organizationId, Long employeeId);
    EmployeeResponseDto getEmployeeById(Long id);
    EmployeeResponseDto getEmployeeByUsername(String username);
    CompleteEmployeeResponse addCompleteEmployee(Integer organizationId, CompleteEmployeeRequest request);

    BulkEmployeeUploadResponse bulkAddCompleteEmployees(Integer organizationId, MultipartFile file);

    List<CompleteEmployeeResponse> getCompleteEmployeesByOrganization(Integer organizationId);

    CompleteEmployeeResponse getCompleteEmployeeById(Integer organizationId, Long employeeId);

    void updateSalaryStructure(Long employeeId, CompleteEmployeeRequest.SalaryStructureRequest salaryRequest);
    CompleteEmployeeResponse updateEmployeeProfile(Long employeeId, UpdateEmployeeRequest request);

    // Organization admin can update employee details
    CompleteEmployeeResponse updateEmployeeDetails(Integer organizationId, Long employeeId, UpdateEmployeeRequest request);

    // Organization admin can update salary structure
    CompleteEmployeeResponse updateEmployeeSalary(Integer organizationId, Long employeeId, UpdateSalaryRequest request);

    // Employee can change their own password
    void changePassword(Long employeeId, ChangePasswordRequest request);

    // Employee can update their bank account (triggers verification)
    CompleteEmployeeResponse updateBankAccount(Long employeeId, UpdateEmployeeRequest.UpdateBankAccountRequest request);

    // Organization admin can deactivate/reactivate employee
    CompleteEmployeeResponse toggleEmployeeStatus(Integer organizationId, Long employeeId, boolean isActive);

    String launchCsvImportJob(Integer organizationId, MultipartFile file) throws Exception;
    CompleteEmployeeResponse getCompleteEmployeeProfileByUsername(String username);
    CompleteEmployeeResponse uploadProfilePicture(Integer organizationId, Long employeeId, MultipartFile file);
}