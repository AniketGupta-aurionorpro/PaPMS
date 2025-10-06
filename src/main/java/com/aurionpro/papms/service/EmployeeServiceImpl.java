package com.aurionpro.papms.service;


import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.repository.*;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.EmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.aurionpro.papms.utils.CsvEmployeeParser;

import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AppUserRepository appUserRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SalaryStructureRepository salaryStructureRepository;
    private final BankAccountRepository bankAccountRepository;
    private final CsvEmployeeParser csvEmployeeParser;
    private static final Logger log = LoggerFactory.getLogger(EmployeeServiceImpl.class);
    private final JobLauncher jobLauncher;
    private final Job employeeCsvImportJob;

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


//    @Override
//    public String launchCsvImportJob(Integer organizationId, MultipartFile file) throws Exception {
//        // 1. Save the file to a temporary location
//        Path tempDir = Files.createTempDirectory("csv-import-");
//        File tempFile = tempDir.resolve(file.getOriginalFilename()).toFile();
//        Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
//        log.info("CSV file saved temporarily to: {}", tempFile.getAbsolutePath());
//
//        // 2. Create JobParameters
//        JobParameters jobParameters = new JobParametersBuilder()
//                .addString("filePath", tempFile.getAbsolutePath())
//                .addLong("timestamp", System.currentTimeMillis()) // Ensures a new job instance
//                .addLong("organizationId", organizationId.longValue()) // Pass organizationId to the job
//                .toJobParameters();
//
//        // 3. Launch the job asynchronously
//        jobLauncher.run(employeeCsvImportJob, jobParameters);
//
//        return "CSV import job started successfully. You will be notified upon completion.";
//    }
    @Override
    public String launchCsvImportJob(Integer organizationId, MultipartFile file) {
    try {
        // Validate organization exists
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        // Save the file to a temporary location
        Path tempDir = Files.createTempDirectory("csv-import-");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String fileName = "employees_" + timestamp + ".csv";
        File tempFile = tempDir.resolve(fileName).toFile();

        Files.copy(file.getInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        log.info("CSV file saved temporarily to: {}", tempFile.getAbsolutePath());

        // Create JobParameters
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("filePath", tempFile.getAbsolutePath())
                .addLong("timestamp", System.currentTimeMillis())
                .addLong("organizationId", organizationId.longValue())
                .toJobParameters();

        // Launch the job asynchronously
        jobLauncher.run(employeeCsvImportJob, jobParameters);

        return "CSV import job started successfully. Processing " + organization.getCompanyName() + "'s employee data.";

    } catch (Exception e) {
        log.error("Failed to start CSV import job for organization {}", organizationId, e);
        throw new RuntimeException("Failed to start CSV import job: " + e.getMessage(), e);
        }
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

    @Override
    @Transactional
    public CompleteEmployeeResponse addCompleteEmployee(Integer organizationId, CompleteEmployeeRequest request) {
        User currentUser = getLoggedInUser();

        // Authorization check
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only Organization Admins can add employees.");
        }
        if (!currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You can only add employees to your own organization.");
        }

        // Validate unique constraints
        if (appUserRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + request.getUsername());
        }
        if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organizationId, request.getEmployeeCode())) {
            throw new DuplicateUserException("Employee code already exists in this organization: " + request.getEmployeeCode());
        }

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        // Step 1: Create and save User
        User newUser = EmployeeMapper.toUserEntity(request, organizationId);
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        User savedUser = appUserRepository.save(newUser);

        // Step 2: Create and save Employee
        Employee newEmployee = EmployeeMapper.toEmployeeEntity(request, savedUser, organization);
        Employee savedEmployee = employeeRepository.save(newEmployee);

        // Step 3: Create and save Bank Account
        BankAccount bankAccount = EmployeeMapper.toBankAccountEntity(request, savedEmployee);
        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
        savedEmployee.setBankAccount(savedBankAccount);

        // Step 4: Create and save Salary Structure
        SalaryStructure salaryStructure = EmployeeMapper.toSalaryStructureEntity(request, savedEmployee);
        SalaryStructure savedSalaryStructure = salaryStructureRepository.save(salaryStructure);
        savedEmployee.setSalaryStructures(List.of(savedSalaryStructure));

        // Send welcome email with credentials
        String subject = "Welcome to " + organization.getCompanyName();
        String body = "<h3>Hello " + request.getFullName() + ",</h3>"
                + "<p>Your employee account has been created successfully.</p>"
                + "<p><b>Username:</b> " + request.getUsername() + "</p>"
                + "<p><b>Temporary Password:</b> " + request.getPassword() + "</p>"
                + "<p><b>Employee Code:</b> " + request.getEmployeeCode() + "</p>"
                + "<p>Please change your password upon first login.</p>";
        emailService.sendEmail(organization.getContactEmail(), request.getEmail(), subject, body);

        return EmployeeMapper.toCompleteDto(savedEmployee);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompleteEmployeeResponse> getCompleteEmployeesByOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You cannot view employees of another organization.");
        }

        organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        return employeeRepository.findByOrganizationId(organizationId).stream()
                .map(EmployeeMapper::toCompleteDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CompleteEmployeeResponse getCompleteEmployeeById(Integer organizationId, Long employeeId) {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        if (!employee.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Employee does not belong to the specified organization.");
        }

        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You are not authorized to access employees of another organization.");
        }

        return EmployeeMapper.toCompleteDto(employee);
    }

    @Override
    @Transactional
    public void updateSalaryStructure(Long employeeId, CompleteEmployeeRequest.SalaryStructureRequest salaryRequest) {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        // Authorization check
        if (currentUser.getRole() == Role.ORG_ADMIN &&
                !currentUser.getOrganizationId().equals(employee.getOrganization().getId())) {
            throw new SecurityException("You can only update salary structures for employees in your organization.");
        }

        // Deactivate current active salary structure if exists
        salaryStructureRepository.findByEmployeeIdAndIsActiveTrue(employeeId)
                .ifPresent(currentSalary -> {
                    currentSalary.setIsActive(false);
                    salaryStructureRepository.save(currentSalary);
                });

        // Create new salary structure
        SalaryStructure newSalaryStructure = SalaryStructure.builder()
                .employee(employee)
                .basicSalary(salaryRequest.getBasicSalary())
                .hra(salaryRequest.getHra())
                .da(salaryRequest.getDa())
                .pfContribution(salaryRequest.getPfContribution())
                .otherAllowances(salaryRequest.getOtherAllowances())
                .effectiveFromDate(salaryRequest.getEffectiveFromDate())
                .isActive(true)
                .build();

        salaryStructureRepository.save(newSalaryStructure);
    }

    // You can also add the bulkAddCompleteEmployees method for CSV uploads
    @Override
    @Transactional
    public BulkEmployeeUploadResponse bulkAddCompleteEmployees(Integer organizationId, MultipartFile file) {
        // Similar to bulkAddEmployees but with complete employee data
        // You'll need to create a CSV format that includes bank account and salary details
        // Implementation would follow similar pattern as bulkAddEmployees
        throw new UnsupportedOperationException("Bulk complete employee upload not yet implemented");
    }

    @Override
    @Transactional
    public BulkEmployeeUploadResponse bulkAddEmployees(Integer organizationId, MultipartFile file) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        // Security validation
        User currentUser = getLoggedInUser();
        if (currentUser.getRole() != Role.ORG_ADMIN || !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You are not authorized to add employees to this organization.");
        }

        // File validation
        validateCsvFile(file);

        // Parse CSV
        CsvEmployeeParser.CsvParseResult parseResult = csvEmployeeParser.parseCsvFile(file);

        List<String> successfulImports = new ArrayList<>();
        List<FailedEmployeeRecord> failedRecords = new ArrayList<>(parseResult.getFailedRecords());
        List<CompleteEmployeeResponse> importedEmployees = new ArrayList<>();

        BigDecimal totalMonthlySalary = BigDecimal.ZERO;
        int employeesWithBankAccounts = 0;
        int employeesWithSalaryStructure = 0;

        // Process valid records
        for (CsvEmployeeRecord csvRecord : parseResult.getValidRecords()) {
            try {
                CompleteEmployeeResponse createdEmployee = processSingleEmployeeRecord(
                        organization, csvRecord, parseResult.getValidRecords().indexOf(csvRecord) + 1
                );

                successfulImports.add(csvRecord.getFullName() + " (" + csvRecord.getEmail() + ")");
                importedEmployees.add(createdEmployee);

                // Update statistics
                if (createdEmployee.getCurrentSalary() != null) {
                    totalMonthlySalary = totalMonthlySalary.add(createdEmployee.getCurrentSalary().getTotalSalary());
                    employeesWithSalaryStructure++;
                }
                if (createdEmployee.getBankAccount() != null) {
                    employeesWithBankAccounts++;
                }

            } catch (Exception e) {
                failedRecords.add(new FailedEmployeeRecord(
                        (long) (parseResult.getValidRecords().indexOf(csvRecord) + 1),
                        convertCsvRecordToMap(csvRecord),
                        "Failed to create employee: " + e.getMessage()
                ));
                log.error("Failed to create employee from CSV record", e);
            }
        }

        // Log bulk upload activity for audit
        log.info("Bulk employee upload completed for organization {}: {} successful, {} failed",
                organizationId, successfulImports.size(), failedRecords.size());

        return BulkEmployeeUploadResponse.builder()
                .successfulImports(successfulImports.size())
                .failedImports(failedRecords.size())
                .message(String.format("Bulk import completed. Success: %d, Failed: %d",
                        successfulImports.size(), failedRecords.size()))
                .successfullyImportedEmployees(successfulImports)
                .failedRecords(failedRecords)
                .importedEmployees(importedEmployees)
                .totalMonthlySalary(totalMonthlySalary)
                .employeesWithBankAccounts(employeesWithBankAccounts)
                .employeesWithSalaryStructure(employeesWithSalaryStructure)
                .build();
    }

    @Transactional
    protected CompleteEmployeeResponse processSingleEmployeeRecord(Organization organization,
                                                                   CsvEmployeeRecord csvRecord,
                                                                   long recordNumber) {
        // Validate unique constraints
        if (appUserRepository.existsByUsername(csvRecord.getUsername())) {
            throw new DuplicateUserException("Username already exists: " + csvRecord.getUsername());
        }
        if (employeeRepository.existsByOrganizationIdAndEmployeeCode(organization.getId(), csvRecord.getEmployeeCode())) {
            throw new DuplicateUserException("Employee code already exists: " + csvRecord.getEmployeeCode());
        }
        if (bankAccountRepository.existsByAccountNumber(csvRecord.getAccountNumber())) {
            throw new DuplicateUserException("Bank account number already exists: " + csvRecord.getAccountNumber());
        }

        // Step 1: Create User
        User newUser = createUserFromCsvRecord(csvRecord, organization.getId());
        User savedUser = appUserRepository.save(newUser);

        // Step 2: Create Employee
        Employee newEmployee = createEmployeeFromCsvRecord(csvRecord, savedUser, organization);
        Employee savedEmployee = employeeRepository.save(newEmployee);

        // Step 3: Create Bank Account
        BankAccount bankAccount = createBankAccountFromCsvRecord(csvRecord, savedEmployee);
        BankAccount savedBankAccount = bankAccountRepository.save(bankAccount);
        savedEmployee.setBankAccount(savedBankAccount);

        // Step 4: Create Salary Structure
        SalaryStructure salaryStructure = createSalaryStructureFromCsvRecord(csvRecord, savedEmployee);
        SalaryStructure savedSalaryStructure = salaryStructureRepository.save(salaryStructure);
        savedEmployee.setSalaryStructures(List.of(savedSalaryStructure));

        // Send welcome email
        sendWelcomeEmail(organization, csvRecord);

        return EmployeeMapper.toCompleteDto(savedEmployee);
    }

    private User createUserFromCsvRecord(CsvEmployeeRecord csvRecord, Integer organizationId) {
        return User.builder()
                .username(csvRecord.getUsername())
                .password(passwordEncoder.encode(csvRecord.getPassword()))
                .fullName(csvRecord.getFullName())
                .email(csvRecord.getEmail())
                .role(Role.EMPLOYEE)
                .organizationId(organizationId)
                .isActive(true)
                .build();
    }

    private Employee createEmployeeFromCsvRecord(CsvEmployeeRecord csvRecord, User user, Organization organization) {
        return Employee.builder()
                .user(user)
                .organization(organization)
                .employeeCode(csvRecord.getEmployeeCode())
                .dateOfJoining(csvRecord.getDateOfJoining())
                .department(csvRecord.getDepartment())
                .jobTitle(csvRecord.getJobTitle())
                .isActive(true)
                .build();
    }

    private BankAccount createBankAccountFromCsvRecord(CsvEmployeeRecord csvRecord, Employee employee) {
        return BankAccount.builder()
                .employee(employee)
                .ownerType(OwnerType.EMPLOYEE)
                .accountHolderName(csvRecord.getAccountHolderName())
                .accountNumber(csvRecord.getAccountNumber())
                .bankName(csvRecord.getBankName())
                .ifscCode(csvRecord.getIfscCode())
                .isPrimary(true)
                .build();
    }

    private SalaryStructure createSalaryStructureFromCsvRecord(CsvEmployeeRecord csvRecord, Employee employee) {
        return SalaryStructure.builder()
                .employee(employee)
                .basicSalary(csvRecord.getBasicSalary())
                .hra(csvRecord.getHra())
                .da(csvRecord.getDa())
                .pfContribution(csvRecord.getPfContribution())
                .otherAllowances(csvRecord.getOtherAllowances())
                .effectiveFromDate(csvRecord.getEffectiveFromDate())
                .isActive(true)
                .build();
    }

    private void sendWelcomeEmail(Organization organization, CsvEmployeeRecord csvRecord) {
        try {
            String subject = "Welcome to " + organization.getCompanyName();
            String body = String.format("""
                <h3>Hello %s,</h3>
                <p>Your employee account has been created successfully.</p>
                <p><b>Username:</b> %s</p>
                <p><b>Employee Code:</b> %s</p>
                <p><b>Department:</b> %s</p>
                <p><b>Job Title:</b> %s</p>
                <p>Please use the temporary password provided to you to log in for the first time.</p>
                <p>We recommend changing your password after first login.</p>
                """,
                    csvRecord.getFullName(), csvRecord.getUsername(), csvRecord.getEmployeeCode(),
                    csvRecord.getDepartment(), csvRecord.getJobTitle());

            emailService.sendEmail(organization.getContactEmail(), csvRecord.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", csvRecord.getEmail(), e);
            // Don't fail the entire process if email fails
        }
    }

    private void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty");
        }

        String contentType = file.getContentType();
        if (!isValidCsvContentType(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid file type. Expected CSV file, got: " + contentType);
        }

        if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
            throw new IllegalArgumentException("File size exceeds maximum limit of 10MB");
        }
    }

    private boolean isValidCsvContentType(String contentType) {
        return contentType != null && (
                contentType.equals("text/csv") ||
                        contentType.equals("application/vnd.ms-excel") ||
                        contentType.equals("application/csv") ||
                        contentType.equals("text/plain")
        );
    }

    private Map<String, String> convertCsvRecordToMap(CsvEmployeeRecord record) {
        Map<String, String> map = new HashMap<>();
        map.put("username", record.getUsername());
        map.put("fullName", record.getFullName());
        map.put("email", record.getEmail());
        map.put("employeeCode", record.getEmployeeCode());
        map.put("department", record.getDepartment());
        map.put("jobTitle", record.getJobTitle());
        map.put("accountNumber", record.getAccountNumber());
        map.put("bankName", record.getBankName());
        return map;
    }
    @Override
    @Transactional
    public CompleteEmployeeResponse updateEmployeeProfile(Long employeeId, UpdateEmployeeRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, true);

        // Employees can only update their own profile
        if (!employee.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only update your own profile");
        }

        return updateEmployeeCommon(employee, request, false);
    }

    @Override
    @Transactional
    public CompleteEmployeeResponse updateEmployeeDetails(Integer organizationId, Long employeeId, UpdateEmployeeRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, false);

        // Organization admin can update any employee in their organization
        validateOrganizationAccess(currentUser, organizationId, employee);

        return updateEmployeeCommon(employee, request, true);
    }

    private CompleteEmployeeResponse updateEmployeeCommon(Employee employee, UpdateEmployeeRequest request, boolean isAdminUpdate) {
        User user = employee.getUser();

        // Update user fields (always allowed)
        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        // Update employee fields (admin only or if allowed for employee)
        if (isAdminUpdate) {
            employee.setDepartment(request.getDepartment());
            employee.setJobTitle(request.getJobTitle());
        }

        // Save updates
        appUserRepository.save(user);
        Employee updatedEmployee = employeeRepository.save(employee);

        log.info("Employee {} updated by user {}", employee.getId(), user.getUsername());

        return EmployeeMapper.toCompleteDto(updatedEmployee);
    }

    @Override
    @Transactional
    public CompleteEmployeeResponse updateEmployeeSalary(Integer organizationId, Long employeeId, UpdateSalaryRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, false);

        // Only organization admin can update salary
        validateOrganizationAccess(currentUser, organizationId, employee);

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization administrators can update salary structures");
        }

        // Deactivate current salary structure
        salaryStructureRepository.findByEmployeeIdAndIsActiveTrue(employeeId)
                .ifPresent(currentSalary -> {
                    currentSalary.setIsActive(false);
                    salaryStructureRepository.save(currentSalary);
                });

        // Create new salary structure
        SalaryStructure newSalary = SalaryStructure.builder()
                .employee(employee)
                .basicSalary(request.getBasicSalary())
                .hra(request.getHra())
                .da(request.getDa())
                .pfContribution(request.getPfContribution())
                .otherAllowances(request.getOtherAllowances())
                .effectiveFromDate(request.getEffectiveFromDate())
                .isActive(true)
                .build();

        salaryStructureRepository.save(newSalary);

        // Log salary change for audit
        log.info("Salary updated for employee {} by admin {}. Reason: {}",
                employeeId, currentUser.getUsername(), request.getChangeReason());

        // Refresh employee with new salary structure
        Employee updatedEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return EmployeeMapper.toCompleteDto(updatedEmployee);
    }

    @Override
    @Transactional
    public void changePassword(Long employeeId, ChangePasswordRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, true);

        // Employees can only change their own password
        if (!employee.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only change your own password");
        }

        User user = employee.getUser();

        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new SecurityException("Current password is incorrect");
        }

        // Validate new password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new SecurityException("New password and confirmation do not match");
        }

        if (request.getNewPassword().length() < 8) {
            throw new SecurityException("New password must be at least 8 characters long");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        appUserRepository.save(user);

        log.info("Password changed for employee {}", employeeId);

        // Send notification email
        try {
            String subject = "Password Changed Successfully";
            String body = String.format("""
                <h3>Hello %s,</h3>
                <p>Your password has been changed successfully.</p>
                <p>If you did not make this change, please contact your administrator immediately.</p>
                """, user.getFullName());

            emailService.sendEmail("noreply@papms.com", user.getEmail(), subject, body);
        } catch (Exception e) {
            log.warn("Failed to send password change notification to {}", user.getEmail(), e);
        }
    }

    // service/EmployeeServiceImpl.java - Remove verification from bank account updates
    @Override
    @Transactional
    public CompleteEmployeeResponse updateBankAccount(Long employeeId, UpdateEmployeeRequest.UpdateBankAccountRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, true);

        // Employees can only update their own bank account
        if (!employee.getUser().getId().equals(currentUser.getId())) {
            throw new SecurityException("You can only update your own bank account");
        }

        BankAccount bankAccount = bankAccountRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new NotFoundException("Bank account not found for employee"));

        // Update bank account details
        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setIfscCode(request.getIfscCode());
        bankAccount.setUpdatedAt(LocalDateTime.now());

        bankAccountRepository.save(bankAccount);

        log.info("Bank account updated for employee {}", employeeId);

        Employee updatedEmployee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found"));

        return EmployeeMapper.toCompleteDto(updatedEmployee);
    }

    @Override
    @Transactional
    public CompleteEmployeeResponse toggleEmployeeStatus(Integer organizationId, Long employeeId, boolean isActive) {
        User currentUser = getLoggedInUser();
        Employee employee = getEmployeeWithAuthorization(employeeId, currentUser, false);

        // Only organization admin can change employee status
        validateOrganizationAccess(currentUser, organizationId, employee);

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization administrators can change employee status");
        }

        employee.setIsActive(isActive);
        employee.getUser().setIsActive(isActive);

        employeeRepository.save(employee);
        appUserRepository.save(employee.getUser());

        String action = isActive ? "activated" : "deactivated";
        log.info("Employee {} {} by admin {}", employeeId, action, currentUser.getUsername());

        return EmployeeMapper.toCompleteDto(employee);
    }

    // Helper methods for authorization and validation
    private Employee getEmployeeWithAuthorization(Long employeeId, User currentUser, boolean allowSelf) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new NotFoundException("Employee not found with ID: " + employeeId));

        // Allow access if:
        // 1. User is ORG_ADMIN in same organization
        // 2. User is the employee themselves (if allowed by the operation)
        boolean isAuthorized = currentUser.getRole() == Role.ORG_ADMIN &&
                currentUser.getOrganizationId().equals(employee.getOrganization().getId());

        boolean isSelf = employee.getUser().getId().equals(currentUser.getId());

        if (!isAuthorized && (!allowSelf || !isSelf)) {
            throw new SecurityException("Access denied to employee record");
        }

        return employee;
    }

    private void validateOrganizationAccess(User currentUser, Integer organizationId, Employee employee) {
        if (currentUser.getRole() != Role.ORG_ADMIN ||
                !currentUser.getOrganizationId().equals(organizationId) ||
                !employee.getOrganization().getId().equals(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    private void notifyAdminAboutBankAccountChange(Employee employee, BankAccount bankAccount) {
        try {
            Organization organization = employee.getOrganization();
            List<User> orgAdmins = appUserRepository.findByOrganizationIdAndRole(
                    organization.getId(), Role.ORG_ADMIN);

            String subject = "Bank Account Update Requires Verification";
            String body = String.format("""
                <h3>Bank Account Update Notification</h3>
                <p>Employee <strong>%s</strong> has updated their bank account details.</p>
                <p><strong>New Bank Details:</strong></p>
                <ul>
                    <li>Account Holder: %s</li>
                    <li>Account Number: %s</li>
                    <li>Bank: %s</li>
                    <li>IFSC: %s</li>
                </ul>
                <p>Please verify these details in the admin panel.</p>
                """,
                    employee.getUser().getFullName(),
                    bankAccount.getAccountHolderName(),
                    bankAccount.getAccountNumber(),
                    bankAccount.getBankName(),
                    bankAccount.getIfscCode());

            for (User admin : orgAdmins) {
                emailService.sendEmail("noreply@papms.com", admin.getEmail(), subject, body);
            }
        } catch (Exception e) {
            log.warn("Failed to send bank account change notification", e);
        }
    }
}