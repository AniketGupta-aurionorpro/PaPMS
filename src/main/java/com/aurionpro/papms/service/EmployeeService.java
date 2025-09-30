package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.BulkEmployeeUploadResponse;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface EmployeeService {
    void addEmployee(Integer organizationId, AddEmployeeRequest request);
    BulkEmployeeUploadResponse bulkAddEmployees(Integer organizationId, MultipartFile file);
    List<EmployeeResponseDto> getEmployeesByOrganization(Integer organizationId);
    EmployeeResponseDto getEmployeeById(Integer organizationId, Long employeeId);
    void deleteEmployee(Integer organizationId, Long employeeId);


    EmployeeResponseDto getEmployeeById(Long id);
    EmployeeResponseDto getEmployeeByUsername(String username);
}