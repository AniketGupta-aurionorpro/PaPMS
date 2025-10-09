package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.PaymentStatus;
import com.aurionpro.papms.Enum.PayrollStatus;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.payroll.CreatePayrollRequest;
import com.aurionpro.papms.dto.payroll.MyPayslipHistoryDto;
import com.aurionpro.papms.dto.payroll.PayrollBatchResponse;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.PayrollMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.EmployeeRepository;
import com.aurionpro.papms.repository.PayrollBatchRepository;
import com.aurionpro.papms.emails.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aurionpro.papms.Enum.Role; // ADD THIS IMPORT
import com.aurionpro.papms.dto.payroll.PayrollPaymentResponse;
import com.aurionpro.papms.repository.PayrollPaymentRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollBatchRepository payrollBatchRepository;
    private final EmployeeRepository employeeRepository;
    private final AppUserRepository userRepository;
    private final TransactionService transactionService;
    private final EmailService emailService;
    private final PayrollPaymentRepository payrollPaymentRepository;

    @Override
    @Transactional
    public PayrollBatchResponse createPayroll(Integer organizationId, CreatePayrollRequest request) {
        User currentUser = getLoggedInUser();
        validateOrgAccess(currentUser, organizationId);

        if (payrollBatchRepository.existsByOrganizationIdAndPayrollMonthAndPayrollYear(
                organizationId, request.getPayrollMonth(), request.getPayrollYear())) {
            throw new IllegalStateException("A payroll for this month and year already exists.");
        }

        List<Employee> activeEmployees = employeeRepository.findByOrganizationId(organizationId).stream()
                .filter(Employee::getIsActive).toList();

        if (activeEmployees.isEmpty()) {
            throw new NotFoundException("No active employees found for this organization.");
        }

        PayrollBatch batch = new PayrollBatch();
        batch.setOrganization(activeEmployees.get(0).getOrganization());
        batch.setPayrollMonth(request.getPayrollMonth());
        batch.setPayrollYear(request.getPayrollYear());
        batch.setStatus(PayrollStatus.PENDING_APPROVAL);
        batch.setSubmittedByUser(currentUser);

        List<PayrollPayment> payments = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (Employee employee : activeEmployees) {
            SalaryStructure currentSalary = employee.getCurrentSalaryStructure();
            if (currentSalary == null) {
                log.warn("Skipping employee {} (ID: {}) as they have no active salary structure.", employee.getUser().getFullName(), employee.getId());
                continue;
            }

            PayrollPayment payment = PayrollPayment.builder()
                    .payrollBatch(batch)
                    .employee(employee)
                    .netSalaryPaid(currentSalary.getTotalSalary())
                    .basicSalary(currentSalary.getBasicSalary())
                    .hra(currentSalary.getHra())
                    .da(currentSalary.getDa())
                    .pfContribution(currentSalary.getPfContribution())
                    .otherAllowances(currentSalary.getOtherAllowances())
                    .status(PaymentStatus.PENDING)
                    .build();
            payments.add(payment);
            totalAmount = totalAmount.add(currentSalary.getTotalSalary());
        }

        if (payments.isEmpty()) {
            throw new IllegalStateException("Payroll creation failed. No employees with active salary structures found.");
        }

        batch.setPayments(payments);
        batch.setTotalAmount(totalAmount);
        batch.setTotalEmployees(payments.size());

        PayrollBatch savedBatch = payrollBatchRepository.save(batch);
        log.info("Created payroll batch {} for organization {} with total amount {}", savedBatch.getId(), organizationId, totalAmount);
        return PayrollMapper.toDto(savedBatch);
    }

    @Override
    @Transactional
    public PayrollBatchResponse approvePayroll(Long batchId) {
        User currentUser = getLoggedInUser();
        PayrollBatch batch = payrollBatchRepository.findByIdWithDetails(batchId)
                .orElseThrow(() -> new NotFoundException("Payroll batch not found with ID: " + batchId));

        if (batch.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending payrolls can be approved.");
        }

        Organization org = batch.getOrganization();
        if (org.getInternalBalance().compareTo(batch.getTotalAmount()) < 0) {
            throw new IllegalStateException("Insufficient funds in organization's account. Required: " +
                    batch.getTotalAmount() + ", Available: " + org.getInternalBalance());
        }

        batch.setStatus(PayrollStatus.PROCESSING);

        Transaction transaction = transactionService.processDebit(
                org,
                batch.getTotalAmount(),
                "Salary payment for " + batch.getPayrollMonth() + "/" + batch.getPayrollYear(),
                TransactionSourceType.PAYROLL_PAYMENT,
                batch.getId()
        );

        batch.setApprovedByUser(currentUser);
        batch.setStatus(PayrollStatus.COMPLETED);
        batch.setTransactionId(transaction.getId());

        batch.getPayments().forEach(payment -> {
            payment.setStatus(PaymentStatus.PROCESSED);
            payment.setProcessedAt(LocalDateTime.now());
        });

        PayrollBatch savedBatch = payrollBatchRepository.save(batch);

        // Send notification email
        emailService.sendEmail(
                "bank-admin@papms.com",
                batch.getSubmittedByUser().getEmail(),
                "Payroll Approved: #" + batch.getId(),
                "Your payroll request for " + batch.getPayrollMonth() + "/" + batch.getPayrollYear() + " has been approved and processed."
        );

        log.info("Payroll batch {} approved by bank admin {}", batch.getId(), currentUser.getUsername());
        return PayrollMapper.toDto(savedBatch);
    }

    @Override
    @Transactional
    public PayrollBatchResponse rejectPayroll(Long batchId, String reason) {
        User currentUser = getLoggedInUser();
        PayrollBatch batch = payrollBatchRepository.findByIdWithDetails(batchId)
                .orElseThrow(() -> new NotFoundException("Payroll batch not found: " + batchId));

        if (batch.getStatus() != PayrollStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only pending payrolls can be rejected.");
        }

        batch.setStatus(PayrollStatus.REJECTED);
        batch.setRejectionReason(reason);
        batch.setApprovedByUser(currentUser);
        PayrollBatch savedBatch = payrollBatchRepository.save(batch);

        emailService.sendEmail(
                "bank-admin@papms.com",
                batch.getSubmittedByUser().getEmail(),
                "Payroll Rejected: #" + batch.getId(),
                "Your payroll request for " + batch.getPayrollMonth() + "/" + batch.getPayrollYear() + " has been rejected. Reason: " + reason
        );

        log.info("Payroll batch {} rejected by bank admin {}. Reason: {}", batchId, currentUser.getUsername(), reason);
        return PayrollMapper.toDto(savedBatch);
    }

    // Other getter methods
    @Override
    @Transactional(readOnly = true)
    public Page<PayrollBatchResponse> getPayrollsForOrganization(Integer organizationId, Pageable pageable) {
        User currentUser = getLoggedInUser();
        validateOrgAccess(currentUser, organizationId);
        return payrollBatchRepository.findByOrganizationId(organizationId, pageable).map(PayrollMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayrollBatchResponse> getPendingPayrolls(Pageable pageable) {
        return payrollBatchRepository.findByStatus(PayrollStatus.PENDING_APPROVAL, pageable).map(PayrollMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollBatchResponse getPayrollById(Long batchId) {
        PayrollBatch batch = payrollBatchRepository.findByIdWithDetails(batchId)
                .orElseThrow(() -> new NotFoundException("Payroll batch not found: " + batchId));
        return PayrollMapper.toDto(batch);
    }

    // Helper Methods
    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    private void validateOrgAccess(User user, Integer organizationId) {
        if (!user.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("Access denied to this organization's resources.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollPaymentResponse getPayrollPaymentDetails(Long paymentId) {
        User currentUser = getLoggedInUser();
        PayrollPayment payment = payrollPaymentRepository.findByIdWithDetails(paymentId)
                .orElseThrow(() -> new NotFoundException("Payroll payment not found with ID: " + paymentId));

        // --- SECURITY CHECK ---
        if (currentUser.getRole() == Role.EMPLOYEE) {
            if (!payment.getEmployee().getUser().getId().equals(currentUser.getId())) {
                throw new SecurityException("Access Denied: You can only view your own payslips.");
            }
        } else if (currentUser.getRole() == Role.ORG_ADMIN) {
            if (!payment.getPayrollBatch().getOrganization().getId().equals(currentUser.getOrganizationId())) {
                throw new SecurityException("Access Denied: You cannot view payslips from another organization.");
            }
        }
        // BANK_ADMIN has implicit access

        return PayrollMapper.toDto(payment);
    }
    @Override
    @Transactional(readOnly = true)
    public Page<MyPayslipHistoryDto> getMyPayslipHistory(Pageable pageable) {
        User currentUser = getLoggedInUser();

        Employee currentEmployee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Employee profile not found for the current user."));

        // Create a new Pageable with default sorting if none is provided
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSortOr(Sort.by("payrollBatch.payrollYear").descending()
                        .and(Sort.by("payrollBatch.payrollMonth").descending()))
        );
        // Call the new paginated repository method
        Page<PayrollPayment> paymentPage = payrollPaymentRepository
                .findByEmployeeIdWithPagination(currentEmployee.getId(), sortedPageable);

        // The .map() function on a Page object preserves pagination details
        return paymentPage.map(payment -> MyPayslipHistoryDto.builder()
                .paymentId(payment.getId())
                .payrollMonth(payment.getPayrollBatch().getPayrollMonth())
                .payrollYear(payment.getPayrollBatch().getPayrollYear())
                .netSalaryPaid(payment.getNetSalaryPaid())
                .status(payment.getStatus().name())
                .build());
    }
}