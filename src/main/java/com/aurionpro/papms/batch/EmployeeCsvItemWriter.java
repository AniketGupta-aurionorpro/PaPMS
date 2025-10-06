
package com.aurionpro.papms.batch;

import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.SalaryStructure;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
public class EmployeeCsvItemWriter implements ItemWriter<Employee> {

    private OrganizationRepository organizationRepository;
    private EmployeeRepository employeeRepository;
    private AppUserRepository appUserRepository;
    private BankAccountRepository bankAccountRepository;
    private SalaryStructureRepository salaryStructureRepository;
    private EmailService emailService;
    private Organization organization;

    // Setter methods for all dependencies
    public void setOrganizationRepository(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public void setEmployeeRepository(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    public void setAppUserRepository(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public void setBankAccountRepository(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public void setSalaryStructureRepository(SalaryStructureRepository salaryStructureRepository) {
        this.salaryStructureRepository = salaryStructureRepository;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));
    }

    @Override
    @Transactional
    public void write(Chunk<? extends Employee> chunk) throws Exception {
        if (organization == null) {
            throw new IllegalStateException("Organization must be set before writing.");
        }

        log.info("Processing chunk of {} employees for organization '{}'",
                chunk.getItems().size(), organization.getCompanyName());

        for (Employee employee : chunk.getItems()) {
            try {
                // Set organization on employee and user
                employee.setOrganization(organization);
                employee.getUser().setOrganizationId(organization.getId());

                // Save user first
                User savedUser = appUserRepository.save(employee.getUser());
                employee.setUser(savedUser);

                // Save employee
                Employee savedEmployee = employeeRepository.save(employee);

                // Save bank account
                if (employee.getBankAccount() != null) {
                    employee.getBankAccount().setEmployee(savedEmployee);
                    bankAccountRepository.save(employee.getBankAccount());
                }

                // Save salary structures
                if (employee.getSalaryStructures() != null && !employee.getSalaryStructures().isEmpty()) {
                    for (SalaryStructure salaryStructure : employee.getSalaryStructures()) {
                        salaryStructure.setEmployee(savedEmployee);
                        salaryStructureRepository.save(salaryStructure);
                    }
                }

                // Send welcome email
                sendWelcomeEmail(savedEmployee);

                log.info("Successfully processed employee: {}", savedEmployee.getUser().getUsername());

            } catch (Exception e) {
                log.error("Failed to process employee: {}", employee.getUser().getUsername(), e);
                // Continue with next employee even if one fails
            }
        }
    }

    private void sendWelcomeEmail(Employee employee) {
        try {
            String subject = "Welcome to " + organization.getCompanyName();
            String body = String.format("""
                <h3>Hello %s,</h3>
                <p>Your employee account has been created successfully via bulk upload.</p>
                <p><b>Username:</b> %s</p>
                <p>Please use the temporary password from the CSV file to log in.</p>
                <p>We recommend changing your password after first login.</p>
                """, employee.getUser().getFullName(), employee.getUser().getUsername());

            emailService.sendEmail(organization.getContactEmail(),
                    employee.getUser().getEmail(), subject, body);

            log.info("Welcome email sent to: {}", employee.getUser().getEmail());

        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}", employee.getUser().getEmail(), e);
        }
    }
}
//
//@Slf4j
//public class EmployeeCsvItemWriter implements ItemWriter<Employee> {
//
//    private OrganizationRepository organizationRepository;
//    private EmployeeRepository employeeRepository;
//    private EmailService emailService;
//    private Organization organization;
//
//    // Setter methods for dependency injection
//    public void setOrganizationRepository(OrganizationRepository organizationRepository) {
//        this.organizationRepository = organizationRepository;
//    }
//
//    public void setEmployeeRepository(EmployeeRepository employeeRepository) {
//        this.employeeRepository = employeeRepository;
//    }
//
//    public void setEmailService(EmailService emailService) {
//        this.emailService = emailService;
//    }
//
//    public void setOrganizationId(Integer organizationId) {
//        this.organization = organizationRepository.findById(organizationId)
//                .orElseThrow(() -> new NotFoundException("Organization not found for batch job: " + organizationId));
//    }
//
//    @Override
//    @Transactional
//    public void write(Chunk<? extends Employee> chunk) throws Exception {
//        if (organization == null) {
//            throw new IllegalStateException("Organization must be set before writing.");
//        }
//
//        log.info("Processing chunk of {} employees for organization '{}'",
//                chunk.getItems().size(), organization.getCompanyName());
//
//        for (Employee employee : chunk.getItems()) {
//            try {
//                // Set organization on employee and user
//                employee.setOrganization(organization);
//                employee.getUser().setOrganizationId(organization.getId());
//
//                // Save the employee (this should cascade to user, bank account, salary structure)
//                employeeRepository.save(employee);
//
//                // Send welcome email
//                sendWelcomeEmail(employee);
//
//                log.info("Successfully processed employee: {}", employee.getUser().getUsername());
//
//            } catch (Exception e) {
//                log.error("Failed to process employee: {}", employee.getUser().getUsername(), e);
//                // Continue with next employee even if one fails
//            }
//        }
//    }
//
//    private void sendWelcomeEmail(Employee employee) {
//        try {
//            String subject = "Welcome to " + organization.getCompanyName();
//            String body = String.format("""
//                <h3>Hello %s,</h3>
//                <p>Your employee account has been created successfully via bulk upload.</p>
//                <p><b>Username:</b> %s</p>
//                <p>Please use the temporary password from the CSV file to log in.</p>
//                <p>We recommend changing your password after first login.</p>
//                """, employee.getUser().getFullName(), employee.getUser().getUsername());
//
//            emailService.sendEmail(organization.getContactEmail(),
//                    employee.getUser().getEmail(), subject, body);
//
//            log.info("Welcome email sent to: {}", employee.getUser().getEmail());
//
//        } catch (Exception e) {
//            log.warn("Failed to send welcome email to {}", employee.getUser().getEmail(), e);
//        }
//    }
//}