package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.dto.OrganizationResponseDto;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.DocumentMapper;
import com.aurionpro.papms.mapper.OrganizationMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.DocumentRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {

    private final OrganizationRepository organizationRepository;
    private static final List<String> ALLOWED_IMAGE_TYPES = List.of("image/jpeg", "image/png");

    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepo;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   PasswordEncoder passwordEncoder, AppUserRepository userRepo,
                                   EmailService emailService, CloudinaryService cloudinaryService,
                                   DocumentRepository documentRepository, ObjectMapper objectMapper) {
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.cloudinaryService = cloudinaryService;
        this.documentRepository = documentRepository;
        this.objectMapper = objectMapper;
    }

    // ADDED HELPER METHOD
    private String sanitizeForFolderName(String name) {
        if (name == null) return "unknown";
        return name.toLowerCase().replaceAll("\\s+", "_").replaceAll("[^a-z0-9_.-]", "");
    }

    @Override
    @Transactional
    public Organization registerOrganizationWithDocuments(String organizationDataJson, MultipartFile document1, MultipartFile document2) {
        OrganizationRegistrationReq request;
        try {
            //converts the JSON string back into the DTO object
            request = objectMapper.readValue(organizationDataJson, OrganizationRegistrationReq.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for organizationData.", e);
        }

        if (organizationRepository.findByCompanyName(request.getCompanyName()).isPresent()) {
            throw new IllegalArgumentException("Company name is already in use.");
        }
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        validateIsPdf(document1);
        validateIsPdf(document2);

        // STEP 1: Create and save the Organization entity with its own details.
        Organization organization = Organization.builder()
                .companyName(request.getCompanyName())
                .contactEmail(request.getEmail())
                .address(request.getAddress())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .build();
        Organization savedOrganization = organizationRepository.save(organization);

        // STEP 2: Create the initial ORG_ADMIN user, disabled by default.
        User orgAdminUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.ORG_ADMIN)
                .organizationId(savedOrganization.getId()) // Link to the new organization
                .isActive(false)
                .requiresPasswordChange(true)
                .build();
        userRepo.save(orgAdminUser);

        // STEP 3: Upload documents and link them to the organization.
        uploadAndLinkDocuments(savedOrganization, document1, document2);

        return savedOrganization;
    }

    @Override
    @Transactional
    // MODIFIED: Added logo parameter
    public Organization registerOrganizationWithDocuments(String organizationDataJson, MultipartFile document1, MultipartFile document2, MultipartFile logo) {
        OrganizationRegistrationReq request;
        try {
            request = objectMapper.readValue(organizationDataJson, OrganizationRegistrationReq.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON format for organizationData.", e);
        }

        if (organizationRepository.findByCompanyName(request.getCompanyName()).isPresent()) {
            throw new IllegalArgumentException("Company name is already in use.");
        }
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        validateIsPdf(document1);
        validateIsPdf(document2);

        // Create the Organization entity
        Organization organization = Organization.builder()
                .companyName(request.getCompanyName())
                .contactEmail(request.getEmail())
                .address(request.getAddress())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .build();

        // --- LOGO UPLOAD LOGIC START ---
        String sanitizedOrgName = sanitizeForFolderName(request.getCompanyName());
        if (logo != null && !logo.isEmpty()) {
            validateIsImage(logo); // Validate the logo file type

            // Upload the logo to Cloudinary in a structured folder
            String folderName = "papms/" + sanitizedOrgName + "/logo";
            Map<String, String> uploadResult = cloudinaryService.uploadFile(logo, folderName);
            String logoUrl = uploadResult.get("url");

            // Set the logo URL on the organization entity
            organization.setLogoUrl(logoUrl);
        }
        // --- LOGO UPLOAD LOGIC END ---

        // Save the Organization to get its ID
        Organization savedOrganization = organizationRepository.save(organization);

        // Create the ORG_ADMIN user
        User orgAdminUser = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .role(Role.ORG_ADMIN)
                .organizationId(savedOrganization.getId())
                .isActive(false)
                .requiresPasswordChange(true)
                .build();
        userRepo.save(orgAdminUser);

        // Upload verification documents and link them to the organization
        uploadAndLinkDocuments(savedOrganization, document1, document2);

        return savedOrganization;
    }

    private void validateIsImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be null or empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Invalid file type for logo: '" + file.getOriginalFilename() + "'. Only JPG, JPEG, and PNG files are allowed.");
        }
    }

    @Override
    public Organization approveOrganization(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));

        if (organization.getStatus() != OrganizationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Organization cannot be approved as its current status is " + organization.getStatus());
        }

        // STEP 1: Find the associated ORG_ADMIN user who is currently disabled.
        // Assuming one ORG_ADMIN per org at registration.
        List<User> users = userRepo.findByOrganizationIdAndRole(organization.getId(), Role.ORG_ADMIN);
        if (users.isEmpty()) {
            throw new IllegalStateException("Cannot approve organization: No ORG_ADMIN user found for organization ID: " + id);
        }
        User orgAdmin = users.get(0);
        orgAdmin.setIsActive(true); // Activate the user.
        userRepo.save(orgAdmin);

        // STEP 2: Update the organization's status and details.
        organization.setStatus(OrganizationStatus.ACTIVE);

        // Generate and assign a unique bank account number
        String newAccountNumber;
        do {
            newAccountNumber = generateUniqueAccountNumber();
        } while (organizationRepository.findByBankAssignedAccountNumber(newAccountNumber).isPresent());
        organization.setBankAssignedAccountNumber(newAccountNumber);

        // Send notification email
        String subject = "Your Organization Registration is Approved";
        String body = "<h3>Congratulations! Your " + organization.getCompanyName() + " organization has been successfully registered and your admin account is now active.</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getContactEmail(), subject, body);

        return organizationRepository.save(organization);
    }

    @Override
    public Organization suspendOrganization(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));

        if (organization.getStatus() != OrganizationStatus.ACTIVE) {
            throw new IllegalStateException("Organization cannot be suspended as its current status is " + organization.getStatus());
        }

        // STEP 1: Suspend the organization record.
        organization.setStatus(OrganizationStatus.SUSPENDED);

        // STEP 2: Disable all associated user accounts for this organization.
        List<User> usersToDisable = new ArrayList<>();
        usersToDisable.addAll(userRepo.findByOrganizationIdAndRole(id, Role.ORG_ADMIN));
        usersToDisable.addAll(userRepo.findByOrganizationIdAndRole(id, Role.EMPLOYEE));

        for (User user : usersToDisable) {
            user.setIsActive(false);
        }
        userRepo.saveAll(usersToDisable);

        String subject = "Your Organization's Account has been Suspended";
        String body = "<h3>Your " + organization.getCompanyName() + " organization services have been suspended by the Bank. Please contact support.</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getContactEmail(), subject, body);

        return organizationRepository.save(organization);
    }

    @Override
    @Transactional(readOnly = true)
    public OrganizationProfileResponse getProfile(Integer id) {
        return organizationRepository.findById(id)
                .map(org -> new OrganizationProfileResponse(
                        org.getId(),
                        org.getCompanyName(),
                        org.getLogoUrl(),
                        org.getContactEmail(), // Use the correct field name
                        org.getStatus().name()))
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));
    }


    private void uploadAndLinkDocuments(Organization organization, MultipartFile document1, MultipartFile document2) {
        String sanitizedOrgName = sanitizeForFolderName(organization.getCompanyName());
        String folderName = "papms/" + sanitizedOrgName + "/verification_docs";
        List<Document> documentsToSave = new ArrayList<>();

        String sanitizedFilename1 = Objects.requireNonNull(document1.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "");
        Map<String, String> uploadResult1 = cloudinaryService.uploadFile(document1, folderName);
        documentsToSave.add(Document.builder()
                .fileName(sanitizedFilename1)
                .cloudinaryUrl(uploadResult1.get("url"))
                .cloudinaryPublicId(uploadResult1.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .organization(organization)
                .uploadedAt(LocalDateTime.now())
                .status(DocumentStatus.Pending)
                .build());

        String sanitizedFilename2 = Objects.requireNonNull(document2.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "");
        Map<String, String> uploadResult2 = cloudinaryService.uploadFile(document2, folderName);
        documentsToSave.add(Document.builder()
                .fileName(sanitizedFilename2)
                .cloudinaryUrl(uploadResult2.get("url"))
                .cloudinaryPublicId(uploadResult2.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .organization(organization)
                .uploadedAt(LocalDateTime.now())
                .status(DocumentStatus.Pending)
                .build());

        documentRepository.saveAll(documentsToSave);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrganizationResponseDto> getAllOrganizations(Pageable pageable) {
        // 1. Call the new paginated repository method to get a page of ACTIVE organizations
        Page<Organization> organizationPage = organizationRepository.findByStatus(OrganizationStatus.ACTIVE, pageable);

        // 2. Map the Page<Organization> to Page<OrganizationResponseDto>
        // We use toSimpleDto to avoid sending nested employee/document lists in a list view
        return organizationPage.map(OrganizationMapper::toSimpleDto);
    }
//    @Override
//    @Transactional(readOnly = true)
//    public List<Organization> getAllOrganizations() {
//        return organizationRepository.findByStatus(OrganizationStatus.ACTIVE);
//    }

    @Override
    public List<Organization> getPendingOrganizations() {
        // MODIFIED: Call the new, correct repository method
        return organizationRepository.findAllByStatus(OrganizationStatus.PENDING_APPROVAL);
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
        // This method is now invalid as username is not on the organization table.
        // It should be removed or refactored to search the user table first.
        // For now, returning empty to prevent errors.
        return Optional.empty();
    }

    @Override
    public Organization rejectOrganization(Integer id, String rejectionReason) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + id));

        if (organization.getStatus() != OrganizationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Organization cannot be rejected as its current status is " + organization.getStatus());
        }

        organization.setStatus(OrganizationStatus.REJECTED);
        organization.setRejectionReason(rejectionReason);

        String subject = "Update on Your Organization Registration";
        String body = "<h3>We regret to inform you that your registration for " + organization.getCompanyName() + " has been rejected. Reason: " + rejectionReason + ".</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getContactEmail(), subject, body);

        return organizationRepository.save(organization);
    }

    private String generateUniqueAccountNumber() {
        long randomPart = (long) (Math.random() * 9_000_000_000L) + 1_000_000_000L;
        return String.valueOf(randomPart);
    }

    @Override
    public List<DocumentResponseDto> uploadVerificationDocuments(Integer organizationId, MultipartFile document1, MultipartFile document2) {
        // This method's logic seems redundant now that registration handles it,
        // but keeping it in case it's used for re-submission.
        validateIsPdf(document1);
        validateIsPdf(document2);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        uploadAndLinkDocuments(organization, document1, document2);

        // Fetch the updated list of documents for the response
        List<Document> updatedDocs = documentRepository.findAll()
                .stream()
                .filter(d -> d.getOrganization().getId().equals(organizationId) && d.getRelatedEntityType() == DocumentType.ORGANIZATION_VERIFICATION)
                .collect(Collectors.toList());

        return updatedDocs.stream()
                .map(DocumentMapper::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateIsPdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be null or empty.");
        }
        if (!"application/pdf".equals(file.getContentType())) {
            throw new IllegalArgumentException("Invalid file type for '" + file.getOriginalFilename() + "'. Only PDF files are allowed.");
        }
    }

}