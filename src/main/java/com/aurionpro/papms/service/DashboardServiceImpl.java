package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.InvoiceStatus;
import com.aurionpro.papms.dto.CompleteEmployeeResponse;
import com.aurionpro.papms.dto.DashboardStatsDto;
import com.aurionpro.papms.dto.EmployeeDashboardDto;
import com.aurionpro.papms.entity.Employee;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.PayrollPayment;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.EmployeeMapper;
import com.aurionpro.papms.mapper.TransactionMapper;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OrganizationRepository organizationRepository;
    private final EmployeeRepository employeeRepository;
    private final VendorRepository vendorRepository;
    private final InvoiceRepository invoiceRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
    private final TransactionRepository transactionRepository;
    private final AppUserRepository userRepository;
    private final PayrollPaymentRepository payrollPaymentRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStatsForOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();
        if (!currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("Access denied to this organization's dashboard.");
        }

        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        LocalDateTime thisMonthStart = LocalDateTime.now().withDayOfMonth(1).toLocalDate().atStartOfDay();

        // Core Stats
        long currentEmployees = employeeRepository.countByOrganizationIdAndIsActiveTrue(organizationId);
        long currentVendors = vendorRepository.countByOrganizationIdAndIsActiveTrue(organizationId);
        long currentPendingInvoices = invoiceRepository.countByOrganizationIdAndStatusNot(organizationId, InvoiceStatus.PAID);

        // Previous period for percentage change
        long previousPendingInvoices = invoiceRepository.countByOrganizationIdAndStatusNotAndCreatedAtBefore(organizationId, InvoiceStatus.PAID, thisMonthStart);

        // Recent Transactions
        Pageable recentTransactionsPageable = PageRequest.of(0, 5, Sort.by("transactionDate").descending());

        return DashboardStatsDto.builder()
                .internalBalance(org.getInternalBalance())
                .totalEmployees(currentEmployees)
                .employeeChangePercentage(0.0) // Placeholder: more complex logic needed for historical tracking
                .totalVendors(currentVendors)
                .vendorChangePercentage(0.0) // Placeholder
                .pendingInvoicesCount(currentPendingInvoices)
                .pendingInvoicesChangePercentage(calculatePercentageChange(previousPendingInvoices, currentPendingInvoices))
                .totalAmountReceivedFromClients(invoiceRepository.findTotalAmountReceivedSince(organizationId, thisMonthStart))
                .totalAmountDueFromClients(invoiceRepository.findTotalAmountDue(organizationId))
                .totalPaidToVendors(vendorPaymentRepository.findTotalPaidSince(organizationId, thisMonthStart.toLocalDate()))
                .totalTransactions(transactionRepository.countByOrganizationIdAndTransactionDateAfter(organizationId, thisMonthStart))
                .totalTransactionVolume(transactionRepository.findTotalVolumeSince(organizationId, thisMonthStart))
                .recentTransactions(transactionRepository.findByOrganizationIdOrderByTransactionDateDesc(organizationId, recentTransactionsPageable)
                        .getContent().stream().map(TransactionMapper::toDto).collect(Collectors.toList()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeDashboardDto getEmployeeDashboardData() {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Employee profile not found for current user."));

        // 1. Get the complete employee profile using existing logic
        CompleteEmployeeResponse profileDto = EmployeeMapper.toCompleteDto(employee);

        // 2. Find the latest payslip
        Optional<PayrollPayment> latestPaymentOpt = payrollPaymentRepository
                .findFirstByEmployeeIdOrderByPayrollBatch_PayrollYearDescPayrollBatch_PayrollMonthDesc(employee.getId());

        // 3. Create the builder, but DO NOT build it yet
        EmployeeDashboardDto.EmployeeDashboardDtoBuilder builder = EmployeeDashboardDto.builder()
                .employeeProfile(profileDto);

        // 4. If a latest payslip exists, add it to the builder
        latestPaymentOpt.ifPresent(payment -> {
            String period = LocalDate.of(
                    payment.getPayrollBatch().getPayrollYear(),
                    payment.getPayrollBatch().getPayrollMonth(),
                    1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));

            EmployeeDashboardDto.LatestPayslipDto latestPayslipDto = EmployeeDashboardDto.LatestPayslipDto.builder()
                    .paymentId(payment.getId())
                    .period(period)
                    .netSalary(payment.getNetSalaryPaid())
                    .build();

            builder.latestPayslip(latestPayslipDto);
        });

        // 5. NOW, build the final object and return it
        return builder.build();
    }

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    private double calculatePercentageChange(long oldValue, long newValue) {
        if (oldValue == 0) {
            return newValue > 0 ? 100.0 : 0.0;
        }
        double change = ((double) (newValue - oldValue) / oldValue) * 100;
        // Round to two decimal places
        return new BigDecimal(change).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}