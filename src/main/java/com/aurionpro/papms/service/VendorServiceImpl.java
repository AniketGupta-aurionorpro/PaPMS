package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.PaymentStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.VendorPaymentRequest;
import com.aurionpro.papms.dto.VendorRequest;
import com.aurionpro.papms.dto.VendorResponse;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.VendorMapper;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {
    private final VendorRepository vendorRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
    private final TransactionService transactionService;

    private User getLoggedInUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    @Override
    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        User currentUser = getLoggedInUser();
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only Organization Admins can create vendors.");
        }

        if (vendorRepository.existsByVendorNameAndOrganizationId(request.getVendorName(), currentUser.getOrganizationId())) {
            throw new DuplicateUserException("A vendor with this name already exists for your organization.");
        }

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        Vendor newVendor = VendorMapper.toEntity(request, organization);
        Vendor savedVendor = vendorRepository.save(newVendor);

        BankAccount bankAccount = BankAccount.builder()
                .ownerId(Math.toIntExact(savedVendor.getId()))
                .ownerType(OwnerType.VENDOR)
                .accountHolderName(request.getAccountHolderName())
                .accountNumber(request.getAccountNumber())
                .bankName(request.getBankName())
                .ifscCode(request.getIfscCode())
                .isPrimary(true)
                .build();
        bankAccountRepository.save(bankAccount);

        return VendorMapper.toDto(savedVendor, bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public VendorResponse getVendorById(Long vendorId) {
        User currentUser = getLoggedInUser();
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with ID: " + vendorId));
        if (!vendor.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to view this vendor.");
        }
        BankAccount bankAccount = bankAccountRepository
                .findByOwnerIdAndOwnerTypeAndIsPrimaryTrue(vendorId, OwnerType.VENDOR)
                .orElse(null);
        return VendorMapper.toDto(vendor, bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorResponse> getVendorsByOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();
        if (currentUser.getRole() == Role.ORG_ADMIN && !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You can only view vendors for your own organization.");
        }
        return vendorRepository.findByOrganizationId(organizationId).stream()
                .map(vendor -> {
                    BankAccount bankAccount = bankAccountRepository
                            .findByOwnerIdAndOwnerTypeAndIsPrimaryTrue(vendor.getId(), OwnerType.VENDOR)
                            .orElse(null);
                    return VendorMapper.toDto(vendor, bankAccount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public VendorResponse updateVendor(Long vendorId, VendorRequest request) {
        User currentUser = getLoggedInUser();
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with ID: " + vendorId));
        if (!vendor.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to update this vendor.");
        }

        VendorMapper.updateEntityFromRequest(vendor, request);
        Vendor savedVendor = vendorRepository.save(vendor);

        BankAccount bankAccount = bankAccountRepository
                .findByOwnerIdAndOwnerTypeAndIsPrimaryTrue(vendor.getId(), OwnerType.VENDOR)
                .orElseThrow(() -> new NotFoundException("Primary bank account not found for this vendor."));

        bankAccount.setAccountHolderName(request.getAccountHolderName());
        bankAccount.setAccountNumber(request.getAccountNumber());
        bankAccount.setBankName(request.getBankName());
        bankAccount.setIfscCode(request.getIfscCode());
        bankAccountRepository.save(bankAccount);

        return VendorMapper.toDto(savedVendor, bankAccount);
    }

    @Override
    @Transactional
    public void deleteVendor(Long vendorId) {
        User currentUser = getLoggedInUser();
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new NotFoundException("Vendor not found with ID: " + vendorId));
        if (!vendor.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to delete this vendor.");
        }
        vendor.setIsActive(false);
        vendorRepository.save(vendor);
    }

    @Override
    @Transactional
    public void processVendorPayment(VendorPaymentRequest request) {
        User currentUser = getLoggedInUser();
        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found for current user."));

        Vendor vendor = vendorRepository.findById(request.getVendorId())
                .orElseThrow(() -> new NotFoundException("Vendor not found with ID: " + request.getVendorId()));

        if (!vendor.getOrganization().getId().equals(organization.getId())) {
            throw new SecurityException("This vendor does not belong to your organization.");
        }
        if (!vendor.getIsActive()) {
            throw new IllegalStateException("Cannot process payment for an inactive vendor.");
        }

        VendorPayment payment = VendorPayment.builder()
                .organization(organization)
                .vendor(vendor)
                .amount(request.getAmount())
                .description(request.getDescription())
                .paymentDate(LocalDate.now())
                .status(PaymentStatus.PENDING)
                .build();
        VendorPayment savedPayment = vendorPaymentRepository.save(payment);

        try {
            String transactionDesc = "Payment to vendor: " + vendor.getVendorName();
            Transaction transaction = transactionService.processDebit(organization, request.getAmount(), transactionDesc, TransactionSourceType.VENDOR_PAYMENT, savedPayment.getId());

            savedPayment.setStatus(PaymentStatus.PROCESSED);
            savedPayment.setTransactionId(transaction.getId());
            vendorPaymentRepository.save(savedPayment);
        } catch (Exception e) {
            savedPayment.setStatus(PaymentStatus.FAILED);
            vendorPaymentRepository.save(savedPayment);
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }
}