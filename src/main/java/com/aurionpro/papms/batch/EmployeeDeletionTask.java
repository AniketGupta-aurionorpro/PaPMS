package com.aurionpro.papms.batch;

import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeDeletionTask {

    private final EmployeeRepository employeeRepository;
    private final AppUserRepository appUserRepository;
    private final BankAccountRepository bankAccountRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final ConcernRepository concernRepository;
    private final PayrollPaymentRepository payrollPaymentRepository;

    /**
     * This scheduled task runs every day at 2:00 AM server time.
     * It finds employees whose deletion was scheduled more than 30 days ago and permanently deletes their data.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void hardDeleteScheduledEmployees() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(30);
        log.info("Running scheduled task to hard-delete employees scheduled for deletion before {}", cutoffDate);

        List<Employee> employeesToDelete = employeeRepository.findAllByDeletionScheduledAtBefore(cutoffDate);

        if (employeesToDelete.isEmpty()) {
            log.info("No employees found for hard deletion.");
            return;
        }

        log.warn("Found {} employees to permanently delete.", employeesToDelete.size());

        for (Employee employee : employeesToDelete) {
            try {
                log.info("Starting hard deletion for employee ID: {}, Username: {}", employee.getId(), employee.getUser().getUsername());

                // Deleting related data in the correct order to respect foreign key constraints

                // 1. PayrollPayment records
                payrollPaymentRepository.deleteInBatch(payrollPaymentRepository.findByEmployeeIdWithPagination(employee.getId(), null).getContent());
                log.debug("Deleted payroll payments for employee ID: {}", employee.getId());

                // 2. Concern records
                concernRepository.deleteInBatch(concernRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()));
                log.debug("Deleted concerns for employee ID: {}", employee.getId());

                // 3. SalaryStructure records
                salaryStructureRepository.deleteInBatch(salaryStructureRepository.findByEmployeeId(employee.getId()));
                log.debug("Deleted salary structures for employee ID: {}", employee.getId());

                // 4. BankAccount records
                bankAccountRepository.findByEmployeeId(employee.getId()).ifPresent(bankAccountRepository::delete);
                log.debug("Deleted bank account for employee ID: {}", employee.getId());

                // 5. Employee record (which will also delete the User due to CascadeType.ALL)
                employeeRepository.delete(employee);
                log.info("Successfully hard-deleted Employee and associated User for ID: {}", employee.getId());

            } catch (Exception e) {
                log.error("Failed to hard-delete employee ID: {}. This employee will be re-processed in the next run.", employee.getId(), e);
                // Continue to the next employee
            }
        }
        log.info("Completed hard deletion task. {} employees were processed for deletion.", employeesToDelete.size());
    }
}