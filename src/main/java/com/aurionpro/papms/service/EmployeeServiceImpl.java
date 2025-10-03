package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.AddEmployeeRequest;
import com.aurionpro.papms.dto.BulkEmployeeUploadResponse;
import com.aurionpro.papms.dto.EmployeeResponseDto;
import com.aurionpro.papms.dto.FailedEmployeeRecord;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.EmployeeMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.EmployeeRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private User getLoggedInUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return appUserRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    @Override
    public void addEmployee(Integer organizationId, AddEmployeeRequest request) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only Organization Admins can add employees.");
        }
        if (!currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You can only add employees to your own organization.");
        }
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + request.getUsername());
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        User newUser = EmployeeMapper.toUserEntity(request, organizationId);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = appUserRepository.saveAndFlush(newUser);

        Employee newEmployee = EmployeeMapper.toEmployeeEntity(request, savedUser, organization);
        employeeRepository.save(newEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmployeeResponseDto> getEmployeesByOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You cannot view employees of another organization.");
        }

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        return employeeRepository.findByOrganizationId(organizationId).stream()
                .map(EmployeeMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeResponseDto getEmployeeById(Integer organizationId, Long employeeId) {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        if (!employee.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Employee does not belong to the specified organization.");
        }

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You are not authorized to access employees of another organization.");
        }

        return EmployeeMapper.toDto(employee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Integer organizationId, Long employeeId) {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        if (!employee.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Employee does not belong to the specified organization.");
        }

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You are not authorized to delete employees of another organization.");
        }

        // Soft delete the employee and user
        employee.setIsActive(false);
        employee.getUser().setIsActive(false);
        employeeRepository.save(employee);
        appUserRepository.save(employee.getUser());
    }


    @Override
    public BulkEmployeeUploadResponse bulkAddEmployees(Integer organizationId, MultipartFile file) {
        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        if (file.isEmpty() || !isCsvFile(file)) {
            throw new IllegalArgumentException("Invalid file. Please upload a non-empty CSV file.");
        }

        List<String> successfulImports = new ArrayList<>();
        List<FailedEmployeeRecord> failedRecords = new ArrayList<>();
        String[] headers = {"fullName", "email", "employeeCode", "department", "jobTitle", "dateOfJoining"};

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(fileReader,
                     CSVFormat.DEFAULT.builder().setHeader(headers).setSkipHeaderRecord(true).setTrim(true).build())) {
        //setHeader(headers): Tells the parser to map columns by name, not by index.
            //setSkipHeaderRecord(true): Ignores the first row.
            //setTrim(true): leading/trailing whitespace from values.

            // iterate providing one row at a time as a CSVRecord object.
            for (CSVRecord csvRecord : csvParser) {
                try {
                    if (csvRecord.size() != headers.length) {
                        failedRecords.add(new FailedEmployeeRecord(csvRecord.getRecordNumber(), csvRecord.toMap(), "Incorrect number of columns."));
                        continue;
                    }

                    String fullName = csvRecord.get("fullName");
                    String email = csvRecord.get("email");
                    String employeeCode = csvRecord.get("employeeCode");

                    if (appUserRepository.findByUsername(email).isPresent()) {
                        failedRecords.add(new FailedEmployeeRecord(csvRecord.getRecordNumber(), csvRecord.toMap(), "Email/Username already exists."));
                        continue;
                    }
                    if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, employeeCode)) {
                        failedRecords.add(new FailedEmployeeRecord(csvRecord.getRecordNumber(), csvRecord.toMap(), "Employee Code already exists for this organization."));
                        continue;
                    }

                    Organization org = organizationRepository.findById(organizationId).get();

                    User newUser = User.builder()
                            .username(email)
                            .password(passwordEncoder.encode(generateTemporaryPassword()))
                            .fullName(fullName)
                            .email(email)
                            .role(Role.EMPLOYEE)
                            .organizationId(organizationId)
                            .isActive(true)
                            .build();
                    User savedUser = appUserRepository.save(newUser);
                    String subject = "Your Organization Registration is Approved";
                    String body = "<h3>Congratulations! Welcome to the  " + org.getCompanyName() + " Your Employee Payroll UserName: "+newUser.getEmail()+" <br/> Password is:"+newUser.getPassword()+" .</h3>";
//                    emailService.sendEmail("Org-email@example.com", newUser.getEmail(), subject, body);

                    Employee newEmployee = Employee.builder()
                            .user(savedUser)
                            .organization(org)
                            .employeeCode(employeeCode)
                            .department(csvRecord.get("department"))
                            .jobTitle(csvRecord.get("jobTitle"))
                            .dateOfJoining(LocalDate.parse(csvRecord.get("dateOfJoining"))) // Expects YYYY-MM-DD
                            .isActive(true)
                            .build();
                    employeeRepository.save(newEmployee);

                    successfulImports.add(fullName + " (" + email + ")");

                } catch (DateTimeParseException e) {
                    failedRecords.add(new FailedEmployeeRecord(csvRecord.getRecordNumber(), csvRecord.toMap(), "Invalid date format for 'dateOfJoining'. Expected YYYY-MM-DD."));
                } catch (Exception e) {
                    failedRecords.add(new FailedEmployeeRecord(csvRecord.getRecordNumber(), csvRecord.toMap(), "An unexpected error occurred: " + e.getMessage()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV file: " + e.getMessage());
        }

        return BulkEmployeeUploadResponse.builder()
                .successfulImports(successfulImports.size())
                .failedImports(failedRecords.size())
                .message("Bulk import process completed.")
                .successfullyImportedEmployees(successfulImports)
                .failedRecords(failedRecords)
                .build();
    }

    @Override
    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + id));
        return EmployeeMapper.toDto(employee);
    }

    @Override
    public EmployeeResponseDto getEmployeeByUsername(String username) {
        User user = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found with username: " + username));
        Employee employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException("Employee details not found for user: " + username));
        return EmployeeMapper.toDto(employee);
    }

    private boolean isCsvFile(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && (contentType.equals("text/csv") || contentType.equals("application/vnd.ms-excel"));
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}