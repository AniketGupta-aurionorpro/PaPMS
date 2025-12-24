package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.Enum.BillStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.vendorDto.BillPaymentRequest;
import com.aurionpro.papms.dto.vendorDto.CreateBillRequest;
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import com.aurionpro.papms.entity.vendorEntity.VendorPayment;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.VendorBillMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.repository.VendorBillRepository;
import com.aurionpro.papms.repository.VendorRepository;
import com.aurionpro.papms.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final VendorBillRepository billRepository;
    private final AppUserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final OrganizationRepository organizationRepository;
    private final TransactionService transactionService;

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    @Override
    @Transactional
    public VendorBillDto generateBillForPayment(VendorPayment payment, Organization organization, Vendor vendor) {
        VendorBill bill = VendorBill.builder()
                .vendorPayment(payment)
                .vendor(vendor)
                .organization(organization)
                .billNumber(generateUniqueBillNumber(organization, vendor))
                .billDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .paidAmount(payment.getAmount())
                .status(BillStatus.PAID)
                .description("Payment to vendor")
                .build();

        VendorBill savedBill = billRepository.save(bill);
        return VendorBillMapper.toDto(savedBill);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorBillDto getBillById(Long billId) {
        User currentUser = getLoggedInUser();
        VendorBill bill = billRepository.findByIdAndOrganizationIdWithDetails(billId, currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Bill not found with ID: " + billId));
        return VendorBillMapper.toDto(bill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBillDto> getAllBillsForOrganization() {
        User currentUser = getLoggedInUser();
        List<VendorBill> bills = billRepository.findByOrganizationId(currentUser.getOrganizationId());
        return bills.stream()
                .map(VendorBillMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorBillDto> getAllBillsForVendor() {
        User currentUser = getLoggedInUser();

        // Find vendor by user
        Vendor vendor = vendorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Vendor profile not found for current user"));

        List<VendorBill> bills = billRepository.findByVendorId(vendor.getId());
        return bills.stream()
                .map(VendorBillMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VendorBillDto createBill(CreateBillRequest request) {
        User currentUser = getLoggedInUser();

        // Ensure user is a vendor
        if (currentUser.getRole() != Role.VENDOR) {
            throw new SecurityException("Only vendors can create bills");
        }

        // Find vendor by user
        Vendor vendor = vendorRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Vendor profile not found for current user"));

        Organization organization = organizationRepository.findById(vendor.getOrganization().getId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        // Create bill
        VendorBill bill = VendorBill.builder()
                .vendor(vendor)
                .organization(organization)
                .billNumber(generateUniqueBillNumber(organization, vendor))
                .billDate(LocalDate.now())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : LocalDate.now().plusDays(30))
                .amount(request.getAmount())
                .paidAmount(BigDecimal.ZERO)
                .status(BillStatus.PENDING)
                .description(request.getDescription())
                .build();

        VendorBill savedBill = billRepository.save(bill);
        return VendorBillMapper.toDto(savedBill);
    }

    @Override
    @Transactional
    public VendorBillDto payBill(BillPaymentRequest request) {
        User currentUser = getLoggedInUser();

        // Ensure user is org admin
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can pay bills");
        }

        VendorBill bill = billRepository
                .findByIdAndOrganizationIdWithDetails(request.getBillId(), currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Bill not found with ID: " + request.getBillId()));

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        String paymentMode = request.getPaymentMode();
        BigDecimal dueAmount = bill.getAmount().subtract(bill.getPaidAmount());

        if ("PAY_LATER".equals(paymentMode)) {
            // Mark as pay later
            bill.setStatus(BillStatus.PAY_LATER);
            if (request.getPayLaterDate() != null) {
                bill.setDueDate(request.getPayLaterDate());
            }
        } else {
            // Full or Partial payment
            BigDecimal paymentAmount = request.getAmount();

            if (paymentAmount == null || paymentAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Payment amount must be greater than 0");
            }

            if (paymentAmount.compareTo(dueAmount) > 0) {
                throw new IllegalArgumentException("Payment amount cannot exceed due amount");
            }

            // Process the debit from organization's account
            String transactionDesc = "Bill payment to vendor: " + bill.getVendor().getVendorName() + " - Bill#: "
                    + bill.getBillNumber();
            Transaction transaction = transactionService.processDebit(organization, paymentAmount, transactionDesc,
                    TransactionSourceType.VENDOR_PAYMENT, bill.getId());

            // Update bill
            BigDecimal newPaidAmount = bill.getPaidAmount().add(paymentAmount);
            bill.setPaidAmount(newPaidAmount);

            if (newPaidAmount.compareTo(bill.getAmount()) >= 0) {
                bill.setStatus(BillStatus.PAID);
            } else {
                bill.setStatus(BillStatus.PARTIALLY_PAID);
            }
        }

        VendorBill savedBill = billRepository.save(bill);
        return VendorBillMapper.toDto(savedBill);
    }

    private String generateUniqueBillNumber(Organization org, Vendor vendor) {
        String orgCode = generateCodeFromName(org.getCompanyName(), 3);
        String vendorCode = generateCodeFromName(vendor.getVendorName(), 3);
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String unique = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        return String.format("BILL-%s-%s-%s-%s", orgCode, vendorCode, datePart, unique);
    }

    private String generateCodeFromName(String name, int length) {
        if (name == null || name.isEmpty()) {
            return "XXX";
        }
        String sanitized = name.replaceAll("[^a-zA-Z0-9]", "").toUpperCase();
        if (sanitized.isEmpty()) {
            return "XXX";
        }
        return sanitized.substring(0, Math.min(sanitized.length(), length));
    }
}