package com.aurionpro.papms.service.vendor;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.Enum.PaymentStatus;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.vendorDto.VendorPaymentRequest;
import com.aurionpro.papms.dto.vendorDto.VendorRequest;
import com.aurionpro.papms.dto.vendorDto.VendorResponse;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import com.aurionpro.papms.entity.vendorEntity.VendorPayment;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.VendorMapper;
import com.aurionpro.papms.repository.*;
import com.aurionpro.papms.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import com.aurionpro.papms.emails.EmailService;

@Service
@RequiredArgsConstructor
public class VendorServiceImpl implements VendorService {

    private final VendorRepository vendorRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final BankAccountRepository bankAccountRepository;
    private final VendorPaymentRepository vendorPaymentRepository;
    private final TransactionService transactionService;
    private final BillService billService;
    private final EmailService emailService;


    private User getLoggedInUser() {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new RuntimeException("Logged-in user not found"));
    }

    @Override
    @Transactional
    public VendorResponse createVendor(VendorRequest request) {
        User currentUser = getLoggedInUser();
        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found for the current user."));

        if (vendorRepository.existsByVendorNameAndOrganizationId(request.getVendorName(), currentUser.getOrganizationId())) {
            throw new DuplicateUserException("A vendor with this name already exists for your organization.");
        }

        Vendor newVendor = VendorMapper.toEntity(request, organization);
        newVendor.setContactPhone(request.getContactPhone());
        Vendor savedVendor = vendorRepository.save(newVendor);

        BankAccount bankAccount = BankAccount.builder()
                .vendor(savedVendor)
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

        // Security Check: Ensure the vendor belongs to the user's organization
        if (!vendor.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to view this vendor.");
        }

        BankAccount bankAccount = bankAccountRepository
                .findByVendorIdAndIsPrimaryTrue(vendor.getId())
                .orElse(null); // It's possible a bank account might not exist, though unlikely with current logic

        return VendorMapper.toDto(vendor, bankAccount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VendorResponse> getVendorsByOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();

        // Security Check: Ensure the user is requesting vendors for their own organization
        if (!currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("You can only view vendors for your own organization.");
        }

        return vendorRepository.findByOrganizationId(organizationId).stream()
                .map(vendor -> {
                    BankAccount bankAccount = bankAccountRepository
                            .findByVendorIdAndIsPrimaryTrue(vendor.getId())
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
                .findByVendorIdAndIsPrimaryTrue(vendor.getId())
                .orElseThrow(() -> new NotFoundException("Primary bank account not found for this vendor. Cannot update details."));

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

        // Security Check
        if (!vendor.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to delete this vendor.");
        }

        // Soft delete by setting the vendor to inactive
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

        // Payment Validations
        if (!vendor.getOrganization().getId().equals(organization.getId())) {
            throw new SecurityException("This vendor does not belong to your organization.");
        }
        if (!vendor.getIsActive()) {
            throw new IllegalStateException("Cannot process payment for an inactive vendor.");
        }

        // Create the initial payment record with a PENDING status
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
            //  Process the debit from the organization's account.
            // This will throw an exception if funds are insufficient.
            String transactionDesc = "Payment to vendor: " + vendor.getVendorName();
            Transaction transaction = transactionService.processDebit(organization, request.getAmount(), transactionDesc, TransactionSourceType.VENDOR_PAYMENT, savedPayment.getId());

            //  If debit is successful, update the payment status to PROCESSED.
            savedPayment.setStatus(PaymentStatus.PROCESSED);
            savedPayment.setTransactionId(transaction.getId());
            vendorPaymentRepository.save(savedPayment);

            //  Generate a "bill" which acts as a historical record/receipt for this payment.
            billService.generateBillForPayment(savedPayment);

            // Send the email notification to the vendor.
            if (vendor.getContactEmail() != null && !vendor.getContactEmail().isBlank()) {
                String subject = "You have received a payment from " + organization.getCompanyName();
                String emailBody = constructPaymentNotificationEmailBody(organization, vendor, savedPayment);
                emailService.sendEmail(organization.getContactEmail(), vendor.getContactEmail(), subject, emailBody);
            }

        } catch (Exception e) {
            // If fails mark the payment as FAILED
            savedPayment.setStatus(PaymentStatus.FAILED);
            vendorPaymentRepository.save(savedPayment);

            // Re-throw the exception so the controller advice can handle it and inform the user.
            throw new RuntimeException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    private String constructPaymentNotificationEmailBody(Organization org, Vendor vendor, VendorPayment payment) {
        return "<html>"
                + "<body>"
                + "<h2>Payment Advice</h2>"
                + "<p>Dear " + vendor.getVendorName() + ",</p>"
                + "<p>This is to inform you that a payment has been processed by <strong>" + org.getCompanyName() + "</strong>.</p>"
                + "<hr>"
                + "<h3>Payment Details:</h3>"
                + "<ul>"
                + "<li><strong>Payment Date:</strong> " + payment.getPaymentDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy")) + "</li>"
                + "<li><strong>Amount:</strong> " + payment.getAmount().toString() + "</li>"
                + "<li><strong>Description:</strong> " + (payment.getDescription() != null && !payment.getDescription().isBlank() ? payment.getDescription() : "N/A") + "</li>"
                + "</ul>"
                + "<hr>"
                + "<p>This is an automated notification. Please contact " + org.getCompanyName() + " directly for any queries.</p>"
                + "</body>"
                + "</html>";
    }
}
