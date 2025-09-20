package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.service.OrganizationService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   PasswordEncoder passwordEncoder) {
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Organization registerOrganization(OrganizationRegistrationReq request) {
        if (organizationRepository.findByCompanyName(request.getCompanyName()).isPresent()) {
            throw new IllegalArgumentException("Company name is already in use.");
        }
        if (organizationRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        Organization organization = Organization.builder()
                .companyName(request.getCompanyName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .address(request.getAddress())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .build();

        return organizationRepository.save(organization);
    }

    @Override
    public List<Organization> getAllOrganizations() {
        return organizationRepository.findAll();
    }

    /**
     * Fetches all organizations that are in a 'PENDING_APPROVAL' state.
     * This method is for the Bank Admin's dashboard to show a queue of new requests.
     */
    @Override
    public List<Organization> getPendingOrganizations() {
        return organizationRepository.findByStatus(OrganizationStatus.PENDING_APPROVAL);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationById(Integer id) {
        return organizationRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationByName(String companyName) {
        return organizationRepository.findByCompanyName(companyName);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Organization> getOrganizationByUsername(String username) {
        return organizationRepository.findByUsername(username);
    }

    @Override
    public Organization approveOrganization(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));

        // You should add a check here to ensure the current status is PENDING_APPROVAL
        if (organization.getStatus() != OrganizationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Organization cannot be approved as its current status is " + organization.getStatus());
        }

        organization.setStatus(OrganizationStatus.ACTIVE);
        // This is where you would also create the initial ORG_ADMIN user and send the invite.
        
     // 2. Generate and assign a unique bank account number
        String newAccountNumber = generateUniqueAccountNumber(); 
        organization.setBankAssignedAccountNumber(newAccountNumber);

        // 3. Save the updated organization
        return organizationRepository.save(organization);
    }

    /**
     * A private helper method to generate a unique account number.
     * You can implement this using a random number generator or a more structured approach.
     */
    private String generateUniqueAccountNumber() {
        // A simple, non-production-grade way to generate a random number.
        // In a real application, you would ensure this number is truly unique
        // and follows a specific bank numbering scheme.
        // Example: Generate a random 9-digit number and prefix it.
        long randomPart = (long) (Math.random() * 9000000000L) + 1000000000L;
        return " " + randomPart;
    }
    

    /**
     * Rejects a pending organization request.
     * The rejection reason is also stored for auditing purposes.
     */
    @Override
    public Organization rejectOrganization(Integer id, String rejectionReason) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));

        // Business rule: only PENDING_APPROVAL organizations can be rejected
        if (organization.getStatus() != OrganizationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Organization cannot be rejected as its current status is " + organization.getStatus());
        }

        organization.setStatus(OrganizationStatus.REJECTED);
        organization.setRejectionReason(rejectionReason); // Set the rejection reason

        return organizationRepository.save(organization);
    }

    /**
     * Suspends an active organization's account.
     * This acts as a soft delete and revokes access.
     */
    @Override
    public Organization suspendOrganization(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));

        // Business rule: only ACTIVE organizations can be suspended
        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Organization cannot be suspended as its current status is " + organization.getStatus());
        }

        organization.setStatus(OrganizationStatus.SUSPENDED);
        // You would also need to disable all associated user accounts here.
        
        return organizationRepository.save(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationProfileResponse getProfile(Integer id) {
        return organizationRepository.findById(id)
                .map(org -> new OrganizationProfileResponse(
                        org.getId(),
                        org.getCompanyName(),
                        org.getEmail(),
                        org.getStatus().name()))
                .orElseThrow(() -> new IllegalArgumentException("Organization not found with ID: " + id));
    }
}