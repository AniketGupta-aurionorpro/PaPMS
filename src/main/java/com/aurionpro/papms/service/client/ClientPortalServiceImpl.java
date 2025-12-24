package com.aurionpro.papms.service.client;

import com.aurionpro.papms.Enum.ClientStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.ClientDto;
import com.aurionpro.papms.dto.OnboardClientRequest;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.ClientPortalMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.ClientRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.security.SecureRandom;

/**
 * Implementation of Client Portal Service
 * Handles client onboarding with auto-generated credentials and email
 * notifications
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientPortalServiceImpl implements ClientPortalService {

    private final ClientRepository clientRepository;
    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$";
    private static final SecureRandom RANDOM = new SecureRandom();

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    /**
     * Generate random username from email
     */
    private String generateUsername(String email) {
        String baseUsername = email.split("@")[0].toLowerCase();
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(8);
        return baseUsername + timestamp;
    }

    /**
     * Generate secure random password
     */
    private String generatePassword() {
        StringBuilder password = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            password.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return password.toString();
    }

    @Override
    @Transactional
    public ClientDto onboardClient(OnboardClientRequest request) {
        User currentUser = getLoggedInUser();

        // Verify user is org admin
        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can onboard clients");
        }

        // Check if email already exists
        if (clientRepository.existsByContactEmail(request.getContactEmail())) {
            throw new DuplicateUserException("A client with email " + request.getContactEmail() + " already exists");
        }

        Organization organization = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found"));

        // Generate credentials
        String username = generateUsername(request.getContactEmail());
        String plainPassword = generatePassword();

        // Ensure unique username
        int counter = 1;
        String finalUsername = username;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + counter++;
        }

        // Create user account
        User clientUser = User.builder()
                .username(finalUsername)
                .password(passwordEncoder.encode(plainPassword))
                .email(request.getContactEmail())
                .fullName(request.getClientName())
                .role(Role.CLIENT)
                .organizationId(organization.getId())
                .isActive(true)
                .requiresPasswordChange(true) // Force password change on first login
                .build();

        User savedUser = userRepository.save(clientUser);

        // Create client entity
        Client client = Client.builder()
                .user(savedUser)
                .organization(organization)
                .clientName(request.getClientName())
                .contactEmail(request.getContactEmail())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .balance(BigDecimal.ZERO)
                .status(ClientStatus.ACTIVE)
                .build();

        Client savedClient = clientRepository.save(client);

        log.info("Client {} onboarded successfully with user ID {}", savedClient.getId(), savedUser.getId());

        // Send welcome email with credentials
        sendWelcomeEmail(savedClient, organization, finalUsername, plainPassword);

        return ClientPortalMapper.toDto(savedClient);
    }

    /**
     * Send welcome email to new client with login credentials
     */
    private void sendWelcomeEmail(Client client, Organization organization, String username, String password) {
        String subject = "Welcome to " + organization.getCompanyName() + " Client Portal";

        String body = String.format(
                "<h2>Welcome to %s Client Portal!</h2>" +
                        "<p>Dear %s,</p>" +
                        "<p>Your account has been created by %s. You can now access the client portal to view invoices and make payments.</p>"
                        +
                        "<h3>Your Login Credentials:</h3>" +
                        "<p><b>Username:</b> %s</p>" +
                        "<p><b>Temporary Password:</b> %s</p>" +
                        "<p><span style='color: red;'><b>IMPORTANT:</b> You will be required to change your password upon first login.</span></p>"
                        +
                        "<p>Please keep these credentials secure and do not share them with anyone.</p>" +
                        "<br>" +
                        "<p>Best regards,<br>%s</p>",
                organization.getCompanyName(),
                client.getClientName(),
                organization.getCompanyName(),
                username,
                password,
                organization.getCompanyName());

        try {
            emailService.sendEmail(
                    organization.getContactEmail(),
                    client.getContactEmail(),
                    subject,
                    body);
            log.info("Welcome email sent to {}", client.getContactEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", client.getContactEmail(), e.getMessage());
            // Don't fail the transaction if email fails
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClientDto> getAllClients(Pageable pageable) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can view all clients");
        }

        return clientRepository.findByOrganizationId(currentUser.getOrganizationId(), pageable)
                .map(ClientPortalMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getClientById(Long clientId) {
        User currentUser = getLoggedInUser();

        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        // Verify client belongs to user's organization
        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Access denied to this client");
        }

        return ClientPortalMapper.toDto(client);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDto getMyProfile() {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() != Role.CLIENT) {
            throw new SecurityException("Only clients can access this profile");
        }

        Client client = clientRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Client profile not found"));

        return ClientPortalMapper.toDto(client);
    }

    @Override
    @Transactional
    public void suspendClient(Long clientId) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can suspend clients");
        }

        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Cannot suspend client from another organization");
        }

        client.setStatus(ClientStatus.SUSPENDED);
        client.getUser().setIsActive(false);
        clientRepository.save(client);

        log.info("Client {} suspended by user {}", clientId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public void activateClient(Long clientId) {
        User currentUser = getLoggedInUser();

        if (currentUser.getRole() != Role.ORG_ADMIN) {
            throw new SecurityException("Only organization admins can activate clients");
        }

        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Cannot activate client from another organization");
        }

        client.setStatus(ClientStatus.ACTIVE);
        client.getUser().setIsActive(true);
        clientRepository.save(client);

        log.info("Client {} activated by user {}", clientId, currentUser.getUsername());
    }

    @Override
    @Transactional(readOnly = true)
    public Long getClientIdByUserId(Long userId) {
        Client client = clientRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Client not found for user ID: " + userId));
        return client.getId();
    }
}
