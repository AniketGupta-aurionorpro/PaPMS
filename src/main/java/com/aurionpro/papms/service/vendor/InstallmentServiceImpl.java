package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.Enum.BillStatus;
import com.aurionpro.papms.Enum.InstallmentFrequency;
import com.aurionpro.papms.Enum.InstallmentStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.vendorDto.CreateInstallmentPlanRequest;
import com.aurionpro.papms.dto.vendorDto.InstallmentDto;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.entity.vendorEntity.BillInstallment;
import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.VendorBillMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.BillInstallmentRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.repository.VendorBillRepository;
import com.aurionpro.papms.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InstallmentServiceImpl implements InstallmentService {

    private final VendorBillRepository billRepository;
    private final BillInstallmentRepository installmentRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final TransactionService transactionService;

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public VendorBillDto createInstallmentPlan(CreateInstallmentPlanRequest request) {
        User currentUser = getLoggedInUser();

        // Ensure user is org admin
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can create installment plans");
        }

        // Find the bill
        VendorBill bill = billRepository.findById(request.getBillId())
                .orElseThrow(() -> new NotFoundException("Bill not found with ID: " + request.getBillId()));

        // Verify bill belongs to user's organization
        if (!bill.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Bill does not belong to your organization");
        }

        // Check bill is eligible for installment plan
        if (bill.getStatus() != BillStatus.PENDING && bill.getStatus() != BillStatus.PAY_LATER) {
            throw new IllegalStateException("Installment plan can only be created for PENDING or PAY_LATER bills");
        }

        // Parse frequency
        InstallmentFrequency frequency;
        try {
            frequency = InstallmentFrequency.valueOf(request.getFrequency().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid frequency. Use: WEEKLY, BI_WEEKLY, or MONTHLY");
        }

        // Calculate installment amounts
        BigDecimal dueAmount = bill.getDueAmount();
        int numInstallments = request.getNumberOfInstallments();
        BigDecimal installmentAmount = dueAmount.divide(BigDecimal.valueOf(numInstallments), 2, RoundingMode.FLOOR);
        BigDecimal remainingAmount = dueAmount
                .subtract(installmentAmount.multiply(BigDecimal.valueOf(numInstallments)));

        // Create installments
        List<BillInstallment> installments = new ArrayList<>();
        LocalDate currentDate = request.getFirstInstallmentDate();

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal amount = installmentAmount;
            // Add remaining amount to last installment
            if (i == numInstallments) {
                amount = amount.add(remainingAmount);
            }

            BillInstallment installment = BillInstallment.builder()
                    .bill(bill)
                    .installmentNumber(i)
                    .amount(amount)
                    .dueDate(currentDate)
                    .status(InstallmentStatus.PENDING)
                    .build();

            installments.add(installment);

            // Calculate next due date
            currentDate = calculateNextDueDate(currentDate, frequency);
        }

        // Save all installments
        installmentRepository.saveAll(installments);

        // Update bill status and installment info
        bill.setStatus(BillStatus.INSTALLMENTS);
        bill.setTotalInstallments(numInstallments);
        bill.setInstallmentFrequency(frequency);
        bill.getInstallments().addAll(installments);

        VendorBill savedBill = billRepository.save(bill);

        log.info("Created {} installments for bill {} with frequency {}",
                numInstallments, bill.getBillNumber(), frequency);

        return VendorBillMapper.toDto(savedBill);
    }

    @Override
    @Transactional
    public VendorBillDto payInstallment(Long installmentId) {
        User currentUser = getLoggedInUser();

        // Ensure user is org admin
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can pay installments");
        }

        // Find installment
        BillInstallment installment = installmentRepository.findById(installmentId)
                .orElseThrow(() -> new NotFoundException("Installment not found with ID: " + installmentId));

        VendorBill bill = installment.getBill();

        // Verify bill belongs to user's organization
        if (!bill.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Bill does not belong to your organization");
        }

        // Check installment is payable
        if (installment.getStatus() == InstallmentStatus.PAID) {
            throw new IllegalStateException("Installment is already paid");
        }

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        // Process the debit
        String transactionDesc = "Installment " + installment.getInstallmentNumber() + "/" +
                bill.getTotalInstallments() + " for Bill#: " + bill.getBillNumber();

        transactionService.processDebit(organization, installment.getAmount(), transactionDesc,
                TransactionSourceType.VENDOR_PAYMENT, bill.getId());

        // Update installment
        installment.setStatus(InstallmentStatus.PAID);
        installment.setPaidDate(LocalDate.now());
        installmentRepository.save(installment);

        // Update bill paid amount
        BigDecimal newPaidAmount = bill.getPaidAmount().add(installment.getAmount());
        bill.setPaidAmount(newPaidAmount);

        // Check if all installments are paid
        if (installmentRepository.areAllInstallmentsPaid(bill.getId())) {
            bill.setStatus(BillStatus.PAID);
            log.info("Bill {} fully paid through installments", bill.getBillNumber());
        }

        VendorBill savedBill = billRepository.save(bill);

        log.info("Paid installment {}/{} for bill {}",
                installment.getInstallmentNumber(), bill.getTotalInstallments(), bill.getBillNumber());

        return VendorBillMapper.toDto(savedBill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentDto> getInstallmentsForBill(Long billId) {
        List<BillInstallment> installments = installmentRepository.findByBillIdOrderByInstallmentNumberAsc(billId);
        return installments.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Scheduled(cron = "0 0 1 * * ?") // Run daily at 1 AM
    public void markOverdueInstallments() {
        LocalDate today = LocalDate.now();
        List<BillInstallment> overdueInstallments = installmentRepository
                .findByStatusAndDueDateBefore(InstallmentStatus.PENDING, today);

        for (BillInstallment installment : overdueInstallments) {
            installment.setStatus(InstallmentStatus.OVERDUE);
            log.warn("Marking installment {} for bill {} as overdue",
                    installment.getInstallmentNumber(), installment.getBill().getBillNumber());
        }

        if (!overdueInstallments.isEmpty()) {
            installmentRepository.saveAll(overdueInstallments);
            log.info("Marked {} installments as overdue", overdueInstallments.size());
        }
    }

    private LocalDate calculateNextDueDate(LocalDate currentDate, InstallmentFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> currentDate.plusWeeks(1);
            case BI_WEEKLY -> currentDate.plusWeeks(2);
            case MONTHLY -> currentDate.plusMonths(1);
        };
    }

    private InstallmentDto toDto(BillInstallment installment) {
        return InstallmentDto.builder()
                .id(installment.getId())
                .billId(installment.getBill().getId())
                .installmentNumber(installment.getInstallmentNumber())
                .amount(installment.getAmount())
                .dueDate(installment.getDueDate())
                .paidDate(installment.getPaidDate())
                .status(installment.getStatus().name())
                .createdAt(installment.getCreatedAt())
                .updatedAt(installment.getUpdatedAt())
                .build();
    }
}
