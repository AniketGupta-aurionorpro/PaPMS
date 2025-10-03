package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.InvoiceStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.emails.EmailService; // Import the EmailService
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.exception.DuplicateUserException;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.ClientMapper;
import com.aurionpro.papms.mapper.InvoiceMapper;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final AppUserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final ClientRepository clientRepository;
    private final InvoiceRepository invoiceRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;
    private final EmailService emailService; // Dependency Injection for EmailService

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Logged-in user not found."));
    }

    @Override
    @Transactional
    public ClientResponseDto createClient(ClientRequestDto request) {
        User currentUser = getLoggedInUser();
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateUserException("Username '" + request.getUsername() + "' is already taken.");
        }
        if (clientRepository.existsByCompanyNameAndOrganizationId(request.getCompanyName(), currentUser.getOrganizationId())) {
            throw new DuplicateUserException("A client with this company name already exists for your organization.");
        }

        Organization org = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found for current user."));

        User newUser = ClientMapper.toUserEntity(request, org, passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(newUser);

        Client newClient = ClientMapper.toClientEntity(request, savedUser, org);
        Client savedClient = clientRepository.save(newClient);

        // EMAIL INTEGRATION: Send a welcome email to the new client
        String subject = "Welcome to " + org.getCompanyName();
        String body = "<h3>Hello " + request.getFullName() + ",</h3>"
                + "<p>An account has been created for you on our payment portal by " + org.getCompanyName() + ".</p>"
                + "<p>You can use the following credentials to log in:</p>"
                + "<p><b>Username:</b> " + request.getUsername() + "</p>"
                + "<p><b>Temporary Password:</b> " + request.getPassword() + "</p>"
                + "<p>We recommend you change your password upon your first login. Thank you!</p>";
        emailService.sendEmail(org.getContactEmail(), request.getEmail(), subject, body);

        return ClientMapper.toDto(savedClient);
    }

    @Override
    @Transactional
    public InvoiceResponseDto createInvoice(InvoiceRequestDto request) {
        User currentUser = getLoggedInUser();
        Organization org = organizationRepository.findById(currentUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Organization not found for current user."));

        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + request.getClientId()));

        if (!client.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("This client does not belong to your organization.");
        }

        if (invoiceRepository.findByInvoiceNumberAndOrganizationId(request.getInvoiceNumber(), org.getId()).isPresent()) {
            throw new DuplicateUserException("Invoice number '" + request.getInvoiceNumber() + "' already exists for your organization.");
        }

        Invoice newInvoice = InvoiceMapper.toEntity(request, org, client);
        Invoice savedInvoice = invoiceRepository.save(newInvoice);

        // EMAIL INTEGRATION: Notify the client about the new invoice
        String clientEmail = client.getUser().getEmail();
        String subject = "New Invoice #" + savedInvoice.getInvoiceNumber() + " from " + org.getCompanyName();
        String body = "<h3>Hello " + client.getCompanyName() + ",</h3>"
                + "<p>A new invoice has been issued to you by " + org.getCompanyName() + ".</p>"
                + "<p><b>Invoice Number:</b> " + savedInvoice.getInvoiceNumber() + "</p>"
                + "<p><b>Amount Due:</b> $" + savedInvoice.getAmount() + "</p>"
                + "<p><b>Due Date:</b> " + savedInvoice.getDueDate() + "</p>"
                + "<p>Please log in to your portal to view and pay the invoice. Thank you.</p>";
        emailService.sendEmail(org.getContactEmail(), clientEmail, subject, body);

        return InvoiceMapper.toDto(savedInvoice);
    }

    @Override
    @Transactional
    public String processInvoicePayment(Integer invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("This invoice has already been paid.");
        }

        Organization organization = invoice.getOrganization();
        Client client = invoice.getClient();

        transactionService.processCredit(
                organization,
                invoice.getAmount(),
                "Payment for invoice #" + invoice.getInvoiceNumber(),
                TransactionSourceType.INVOICE,
                Long.valueOf(invoice.getId())
        );

        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        // EMAIL 1: Send payment confirmation to the client
        String clientEmail = client.getUser().getEmail();
        String clientSubject = "Payment Confirmation for Invoice #" + invoice.getInvoiceNumber();
        String clientBody = "<h3>Dear " + client.getCompanyName() + ",</h3>"
                + "<p>This is a confirmation that we have successfully received your payment of <b>$" + invoice.getAmount() + "</b> for invoice #" + invoice.getInvoiceNumber() + ".</p>"
                + "<p>Thank you for your business!</p>";
        emailService.sendEmail(organization.getContactEmail(), clientEmail, clientSubject, clientBody);

        // EMAIL 2: Notify the organization that a payment was received
        String orgEmail = organization.getContactEmail();
        String orgSubject = "Payment Received for Invoice #" + invoice.getInvoiceNumber();
        String orgBody = "<h3>Payment Notification</h3>"
                + "<p>Payment of <b>$" + invoice.getAmount() + "</b> has been received from " + client.getCompanyName() + " for invoice #" + invoice.getInvoiceNumber() + ".</p>"
                + "<p>The organization's internal balance has been updated accordingly.</p>";
        emailService.sendEmail("no-reply@papms.com", orgEmail, orgSubject, orgBody);


        return "Payment for invoice " + invoice.getInvoiceNumber() + " processed successfully.";
    }

    // ... (the rest of the methods: getClientById, getAllClientsForCurrentOrg, etc., remain unchanged)

    @Override
    @Transactional(readOnly = true)
    public ClientResponseDto getClientById(Integer clientId) {
        User currentUser = getLoggedInUser();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to view this client.");
        }
        return ClientMapper.toDto(client);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponseDto> getAllClientsForCurrentOrg() {
        User currentUser = getLoggedInUser();
        return clientRepository.findByOrganizationId(currentUser.getOrganizationId()).stream()
                .map(ClientMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClientResponseDto updateClient(Integer clientId, ClientRequestDto request) {
        User currentUser = getLoggedInUser();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to update this client.");
        }

        client.setCompanyName(request.getCompanyName());
        client.setContactPerson(request.getContactPerson());

        User user = client.getUser();
        user.setFullName(request.getFullName());

        clientRepository.save(client);
        userRepository.save(user);

        return ClientMapper.toDto(client);
    }

    @Override
    @Transactional
    public void toggleClientStatus(Integer clientId, boolean isActive) {
        User currentUser = getLoggedInUser();
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        if (!client.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("You are not authorized to modify this client.");
        }

        client.setActive(isActive);
        client.getUser().setIsActive(isActive);

        clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDto getInvoiceById(Integer invoiceId) {
        User currentUser = getLoggedInUser();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException("Invoice not found with ID: " + invoiceId));

        if (currentUser.getRole() == Role.ORG_ADMIN && !invoice.getOrganization().getId().equals(currentUser.getOrganizationId())) {
            throw new SecurityException("Access denied to this invoice.");
        }
        if (currentUser.getRole() == Role.CLIENT) {
            Client client = clientRepository.findByUserId(currentUser.getId())
                    .orElseThrow(() -> new NotFoundException("Client profile not found for current user."));
            if (!invoice.getClient().getId().equals(client.getId())) {
                throw new SecurityException("Access denied. This invoice does not belong to you.");
            }
        }
        return InvoiceMapper.toDto(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoicesForClient(Integer clientId) {
        return invoiceRepository.findByClientId(clientId).stream()
                .map(InvoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDto> getAllInvoicesForCurrentOrg() {
        User currentUser = getLoggedInUser();
        return invoiceRepository.findByOrganizationId(currentUser.getOrganizationId()).stream()
                .map(InvoiceMapper::toDto)
                .collect(Collectors.toList());
    }
}