package com.aurionpro.papms.service.vendor;

// No BillStatus import
import com.aurionpro.papms.dto.vendorDto.VendorBillDto;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.entity.vendorEntity.Vendor;
import com.aurionpro.papms.entity.vendorEntity.VendorBill;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.vendorEntity.VendorPayment;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.VendorBillMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.VendorBillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private final VendorBillRepository billRepository;
    private final AppUserRepository userRepository;

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));
    }

    @Override
    @Transactional
    // MODIFIED: Use the new parameters
    public VendorBillDto generateBillForPayment(VendorPayment payment, Organization organization, Vendor vendor) {
        VendorBill bill = VendorBill.builder()
                .vendorPayment(payment)
                .vendor(vendor) // Use the passed vendor
                .organization(organization) // Use the passed organization
                .billNumber(generateUniqueBillNumber(payment, organization, vendor)) // Pass them down
                .billDate(payment.getPaymentDate())
                .amount(payment.getAmount())
                .build();

        VendorBill savedBill = billRepository.save(bill);
        return VendorBillMapper.toDto(savedBill);
    }
//    @Override
//    @Transactional
//    public VendorBillDto generateBillForPayment(VendorPayment payment) {
//        VendorBill bill = VendorBill.builder()
//                .vendorPayment(payment)
//                .vendor(payment.getVendor())
//                .organization(payment.getOrganization())
//                .billNumber(generateUniqueBillNumber(payment))
//                .billDate(payment.getPaymentDate())
//                .amount(payment.getAmount())
//                .build();
//
//        VendorBill savedBill = billRepository.save(bill);
//        return VendorBillMapper.toDto(savedBill);
//    }

    @Override
    @Transactional(readOnly = true)
    public VendorBillDto getBillById(Long billId) {
        User currentUser = getLoggedInUser();
//        VendorBill bill = billRepository.findByIdAndOrganizationId(billId, currentUser.getOrganizationId())
//                .orElseThrow(() -> new NotFoundException("Bill not found with ID: " + billId));
//        return VendorBillMapper.toDto(bill);

        //new for org name in vendor bill
        VendorBill bill = billRepository.findByIdAndOrganizationIdWithDetails(billId, currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Bill not found with ID: " + billId + " for your organization."));
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

    // MODIFIED: This method now also accepts the fully loaded objects
    private String generateUniqueBillNumber(VendorPayment payment, Organization org, Vendor vendor) {
        // We no longer need to get them from the payment object
        // Organization org = payment.getOrganization();
        // Vendor vendor = payment.getVendor();

        String orgCode = generateCodeFromName(org.getCompanyName(), 3);
        String vendorCode = generateCodeFromName(vendor.getVendorName(), 3);
        String datePart = payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Long paymentId = payment.getId();

        if (paymentId == null) {
            throw new IllegalStateException("Payment ID cannot be null when generating a bill number.");
        }

        return String.format("%s-%s-%s-%d", orgCode, vendorCode, datePart, paymentId);
    }


//    private String generateUniqueBillNumber(VendorPayment payment) {
//        Organization org = payment.getOrganization();
//        Vendor vendor = payment.getVendor();
//
//        String orgCode = generateCodeFromName(org.getCompanyName(), 3);
//        String vendorCode = generateCodeFromName(vendor.getVendorName(), 3);
//        String datePart = payment.getPaymentDate().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        Long paymentId = payment.getId();
//
//        if (paymentId == null) {
//            throw new IllegalStateException("Payment ID cannot be null when generating a bill number.");
//        }
//
//        return String.format("%s-%s-%s-%d", orgCode, vendorCode, datePart, paymentId);
//    }

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