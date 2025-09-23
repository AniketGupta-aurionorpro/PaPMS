package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.RegisterRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.aurionpro.papms.Enum.DocumentStatus;
import com.aurionpro.papms.Enum.DocumentType;
import com.aurionpro.papms.Enum.OrganizationStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.DocumentResponseDto;
import com.aurionpro.papms.dto.OrganizationProfileResponse;
import com.aurionpro.papms.dto.OrganizationRegistrationReq;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Document;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.DocumentRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class OrganizationServiceImpl implements OrganizationService {


    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserRepository userRepo;
    private final EmailService emailService;
    private final CloudinaryService cloudinaryService;
    private final DocumentRepository documentRepository;
    private final ObjectMapper objectMapper;

    public OrganizationServiceImpl(OrganizationRepository organizationRepository,
                                   PasswordEncoder passwordEncoder, AppUserRepository userRepo,
                                   EmailService emailService, CloudinaryService cloudinaryService,
                                   DocumentRepository documentRepository, ObjectMapper objectMapper) { // <-- Add to constructor
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.cloudinaryService = cloudinaryService;
        this.documentRepository = documentRepository; // <-- Initialize
        this.objectMapper = objectMapper;
    }


    @Override
    public Organization registerOrganizationWithDocuments(String organizationDataJson, MultipartFile document1, MultipartFile document2) {
        OrganizationRegistrationReq request;
        try {
            // Manually deserialize the JSON string to our DTO
            request = objectMapper.readValue(organizationDataJson, OrganizationRegistrationReq.class);
        } catch (JsonProcessingException e) {
            // If JSON is malformed, it's a client error (Bad Request)
            throw new IllegalArgumentException("Invalid JSON format for organizationData.", e);
        }

        // --- 1. VALIDATE INCOMING DATA (from the deserialized request object) ---
        if (organizationRepository.findByCompanyName(request.getCompanyName()).isPresent()) {
            throw new IllegalArgumentException("Company name is already in use.");
        }
        if (organizationRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }
        validateIsPdf(document1);
        validateIsPdf(document2);

        // --- 2. CREATE AND SAVE THE ORGANIZATION FIRST TO GET AN ID ---
        Organization organization = Organization.builder()
                .companyName(request.getCompanyName())
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .email(request.getEmail())
                .address(request.getAddress())
                .status(OrganizationStatus.PENDING_APPROVAL)
                .build();
        Organization savedOrganization = organizationRepository.save(organization);

        // --- 3. UPLOAD FILES AND CREATE DOCUMENT RECORDS ---
        uploadAndLinkDocuments(savedOrganization, document1, document2);

        return savedOrganization;
    }


    // A private helper to avoid code duplication
    private void uploadAndLinkDocuments(Organization organization, MultipartFile document1, MultipartFile document2) {
        String folderName = "organization-verification/" + organization.getCompanyName().replaceAll("\\s+", "_").toLowerCase();
        List<Document> documentsToSave = new ArrayList<>();

        // Sanitize, upload, and build first document entity
        String sanitizedFilename1 = document1.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "");
        Map<String, String> uploadResult1 = cloudinaryService.uploadFile(document1, folderName);
        documentsToSave.add(Document.builder()
                .fileName(sanitizedFilename1)
                .cloudinaryUrl(uploadResult1.get("url"))
                .cloudinaryPublicId(uploadResult1.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .relatedEntityId(organization.getId())
                .status(DocumentStatus.Pending)
                .build());

        // Sanitize, upload, and build second document entity
        String sanitizedFilename2 = document2.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "");
        Map<String, String> uploadResult2 = cloudinaryService.uploadFile(document2, folderName);
        documentsToSave.add(Document.builder()
                .fileName(sanitizedFilename2)
                .cloudinaryUrl(uploadResult2.get("url"))
                .cloudinaryPublicId(uploadResult2.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .relatedEntityId(organization.getId())
                .status(DocumentStatus.Pending)
                .build());

        documentRepository.saveAll(documentsToSave);
    }

    @Override
    public List<Organization> getAllOrganizations() {

        List<Organization> orgList = organizationRepository.findAll();
        return orgList.stream().filter(org -> org.getStatus() == OrganizationStatus.ACTIVE).toList();
    }

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
        User u = User.builder().username(organization.getUsername()).password(organization.getPassword()).fullName(organization.getFullName()).email(organization.getEmail()).role(Role.ORG_ADMIN).organizationId(organization.getId()).enable(true).build();
        userRepo.save(u);
     // 2. Generate and assign a unique bank account number
        String newAccountNumber = "";
        do {
            newAccountNumber = generateUniqueAccountNumber();
        } while(organizationRepository.findByBankAssignedAccountNumber(newAccountNumber).isPresent());
        
        organization.setBankAssignedAccountNumber(newAccountNumber);
        String subject = "Your Organization Registration is Approved";
        String body = "<h3>Congratulations! Your "+ organization.getCompanyName()+"  organization has been successfully registered with our bank.</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getEmail(), subject, body);
        // 3. Save the updated organization
        return organizationRepository.save(organization);
    }

    /**
     * A private helper method to generate a unique account number.
     * You can implement this using a random number generator or a more structured approach.
     */
    private String generateUniqueAccountNumber() {
         long randomPart = (long) (Math.random() * 9000000000L) + 1000000000L;
        return randomPart+"";
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

        String subject = "Your Organization Registration is Rejected";
        String body = "<h3> Your "+ organization.getCompanyName()+"  organization has been Rejected due to "+rejectionReason+" .</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getEmail(), subject, body);


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
        String subject = "Your Organization is been suspended";
        String body = "<h3>Your "+ organization.getCompanyName()+"  organization services has been suspended by the Bank.</h3>";
        emailService.sendEmail("bank-email@example.com", organization.getEmail(), subject, body);

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

    @Override
    public List<DocumentResponseDto> uploadVerificationDocuments(Integer organizationId, MultipartFile document1, MultipartFile document2) {

        validateIsPdf(document1);
        validateIsPdf(document2);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        String folderName = "organization-verification/" + organization.getCompanyName().replaceAll("\\s+", "_").toLowerCase();

        List<Document> uploadedDocuments = new ArrayList<>();

        // --- SANITIZE THE FILENAMES ---
        // This regex removes any character that is NOT a letter, number, underscore, hyphen, or period.
        String sanitizedFilename1 = Objects.requireNonNull(document1.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "");
        String sanitizedFilename2 = Objects.requireNonNull(document2.getOriginalFilename()).replaceAll("[^a-zA-Z0-9._-]", "");


        // Process the first document
        Map<String, String> uploadResult1 = cloudinaryService.uploadFile(document1, folderName);
        Document doc1 = Document.builder()
                .fileName(sanitizedFilename1) // Use the sanitized filename
                .cloudinaryUrl(uploadResult1.get("url"))
                .cloudinaryPublicId(uploadResult1.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .relatedEntityId(organization.getId())
                .status(DocumentStatus.Pending)
                .build();
        uploadedDocuments.add(doc1);

        // Process the second document
        Map<String, String> uploadResult2 = cloudinaryService.uploadFile(document2, folderName);
        Document doc2 = Document.builder()
                .fileName(sanitizedFilename2) // Use the sanitized filename
                .cloudinaryUrl(uploadResult2.get("url"))
                .cloudinaryPublicId(uploadResult2.get("public_id"))
                .relatedEntityType(DocumentType.ORGANIZATION_VERIFICATION)
                .relatedEntityId(organization.getId())
                .status(DocumentStatus.Pending)
                .build();
        uploadedDocuments.add(doc2);

        List<Document> savedDocuments = documentRepository.saveAll(uploadedDocuments);
        return savedDocuments.stream()
                .map(DocumentResponseDto::fromEntity)
                .collect(Collectors.toList());
    }
    private void validateIsPdf(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty.");
        }
        // "application/pdf" is the standard MIME type for PDF files.
        if (!Objects.equals(file.getContentType(), "application/pdf")) {
            throw new IllegalArgumentException("Invalid file type for '" + file.getOriginalFilename() + "'. Only PDF files are allowed.");
        }
    }
}