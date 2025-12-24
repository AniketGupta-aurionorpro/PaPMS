package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.ClientDepositStatus;
import com.aurionpro.papms.dto.ClientDepositRequestDto;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.Client;
import com.aurionpro.papms.entity.ClientDepositRequest;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.ClientDepositRequestRepository;
import com.aurionpro.papms.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of ClientDepositService
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ClientDepositServiceImpl implements ClientDepositService {

    private final ClientDepositRequestRepository depositRequestRepository;
    private final ClientRepository clientRepository;
    private final EmailService emailService;

    @Override
    public ClientDepositRequestDto.Response createDepositRequest(
            ClientDepositRequestDto.CreateRequest request, Long clientId) {

        log.info("Creating deposit request for client {}, amount: {}", clientId, request.getAmount());

        Client client = clientRepository.findById(clientId.intValue())
                .orElseThrow(() -> new NotFoundException("Client not found with ID: " + clientId));

        ClientDepositRequest depositRequest = ClientDepositRequest.builder()
                .client(client)
                .organization(client.getOrganization())
                .amount(request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .remarks(request.getRemarks())
                .status(ClientDepositStatus.APPROVED) // Auto-approved
                .processedAt(LocalDateTime.now())
                .approvedBy(null) // System approved
                .build();

        depositRequest = depositRequestRepository.save(depositRequest);

        // Update client balance immediately
        BigDecimal newBalance = client.getBalance().add(request.getAmount());
        client.setBalance(newBalance);
        clientRepository.save(client);

        log.info("Deposit request {} created and AUTO-APPROVED successfully. Client {} balance updated.",
                depositRequest.getId(), clientId);

        // Send approval email immediately
        sendApprovalEmail(depositRequest);

        return toResponseDto(depositRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDepositRequestDto.Response> getClientDepositRequests(Long clientId) {
        return depositRequestRepository.findByClientIdOrderByCreatedAtDesc(clientId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDepositRequestDto.Response> getOrganizationDepositRequests(Integer organizationId) {
        return depositRequestRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientDepositRequestDto.Response> getPendingDepositRequests(Integer organizationId) {
        return depositRequestRepository.findByOrganizationIdAndStatusOrderByCreatedAtDesc(
                organizationId, ClientDepositStatus.PENDING)
                .stream()
                .map(this::toResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public ClientDepositRequestDto.Response approveDeposit(Long depositId, Integer approvedBy) {
        log.info("Approving deposit request {} by user {}", depositId, approvedBy);

        ClientDepositRequest deposit = depositRequestRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit request not found with ID: " + depositId));

        if (deposit.getStatus() != ClientDepositStatus.PENDING) {
            throw new IllegalStateException("Deposit request is not pending. Current status: " + deposit.getStatus());
        }

        // Update client balance
        Client client = deposit.getClient();
        BigDecimal newBalance = client.getBalance().add(deposit.getAmount());
        client.setBalance(newBalance);
        clientRepository.save(client);
        log.info("Client {} balance updated. New balance: {}", client.getId(), newBalance);

        // Update deposit request
        deposit.setStatus(ClientDepositStatus.APPROVED);
        deposit.setApprovedBy(approvedBy);
        deposit.setProcessedAt(LocalDateTime.now());
        deposit = depositRequestRepository.save(deposit);

        // Send email notification
        sendApprovalEmail(deposit);

        log.info("Deposit request {} approved successfully", depositId);
        return toResponseDto(deposit);
    }

    @Override
    public ClientDepositRequestDto.Response rejectDeposit(Long depositId, String reason, Integer rejectedBy) {
        log.info("Rejecting deposit request {} by user {}", depositId, rejectedBy);

        ClientDepositRequest deposit = depositRequestRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit request not found with ID: " + depositId));

        if (deposit.getStatus() != ClientDepositStatus.PENDING) {
            throw new IllegalStateException("Deposit request is not pending. Current status: " + deposit.getStatus());
        }

        deposit.setStatus(ClientDepositStatus.REJECTED);
        deposit.setApprovedBy(rejectedBy);
        deposit.setRejectionReason(reason);
        deposit.setProcessedAt(LocalDateTime.now());
        deposit = depositRequestRepository.save(deposit);

        // Send email notification
        sendRejectionEmail(deposit, reason);

        log.info("Deposit request {} rejected", depositId);
        return toResponseDto(deposit);
    }

    @Override
    @Transactional(readOnly = true)
    public ClientDepositRequestDto.Response getDepositById(Long depositId) {
        ClientDepositRequest deposit = depositRequestRepository.findById(depositId)
                .orElseThrow(() -> new NotFoundException("Deposit request not found with ID: " + depositId));
        return toResponseDto(deposit);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingCount(Integer organizationId) {
        return depositRequestRepository.countByOrganizationIdAndStatus(organizationId, ClientDepositStatus.PENDING);
    }

    private ClientDepositRequestDto.Response toResponseDto(ClientDepositRequest deposit) {
        return ClientDepositRequestDto.Response.builder()
                .id(deposit.getId())
                .clientId(deposit.getClient().getId())
                .clientName(deposit.getClient().getClientName())
                .clientEmail(deposit.getClient().getContactEmail())
                .organizationId(deposit.getOrganization().getId())
                .organizationName(deposit.getOrganization().getCompanyName())
                .amount(deposit.getAmount())
                .referenceNumber(deposit.getReferenceNumber())
                .remarks(deposit.getRemarks())
                .status(deposit.getStatus())
                .rejectionReason(deposit.getRejectionReason())
                .processedAt(deposit.getProcessedAt())
                .createdAt(deposit.getCreatedAt())
                .updatedAt(deposit.getUpdatedAt())
                .build();
    }

    private void sendApprovalEmail(ClientDepositRequest deposit) {
        try {
            String clientEmail = deposit.getClient().getContactEmail();
            String clientName = deposit.getClient().getClientName();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

            String htmlBody = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #22c55e;">Deposit Request Approved ✓</h2>
                            <p>Dear %s,</p>
                            <p>Your deposit request has been <strong style="color: #22c55e;">approved</strong>.</p>
                            <div style="background: #f0fdf4; padding: 20px; border-radius: 8px; border-left: 4px solid #22c55e;">
                                <p><strong>Amount:</strong> ₹%s</p>
                                <p><strong>Reference:</strong> %s</p>
                                <p><strong>Approved on:</strong> %s</p>
                            </div>
                            <p>The amount has been credited to your wallet balance.</p>
                            <p>Best regards,<br><strong>%s</strong></p>
                        </div>
                    </body>
                    </html>
                    """
                    .formatted(
                            clientName,
                            deposit.getAmount().toPlainString(),
                            deposit.getReferenceNumber() != null ? deposit.getReferenceNumber() : "N/A",
                            deposit.getProcessedAt().format(formatter),
                            deposit.getOrganization().getCompanyName());

            emailService.sendEmail("noreply@papms.com", clientEmail, "Deposit Request Approved", htmlBody);
        } catch (Exception e) {
            log.error("Failed to send approval email: {}", e.getMessage());
        }
    }

    private void sendRejectionEmail(ClientDepositRequest deposit, String reason) {
        try {
            String clientEmail = deposit.getClient().getContactEmail();
            String clientName = deposit.getClient().getClientName();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

            String htmlBody = """
                    <html>
                    <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                        <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                            <h2 style="color: #dc2626;">Deposit Request Rejected</h2>
                            <p>Dear %s,</p>
                            <p>Unfortunately, your deposit request has been <strong style="color: #dc2626;">rejected</strong>.</p>
                            <div style="background: #fef2f2; padding: 20px; border-radius: 8px; border-left: 4px solid #dc2626;">
                                <p><strong>Amount:</strong> ₹%s</p>
                                <p><strong>Reference:</strong> %s</p>
                                <p><strong>Reason:</strong> %s</p>
                                <p><strong>Rejected on:</strong> %s</p>
                            </div>
                            <p>Please contact us if you have any questions.</p>
                            <p>Best regards,<br><strong>%s</strong></p>
                        </div>
                    </body>
                    </html>
                    """
                    .formatted(
                            clientName,
                            deposit.getAmount().toPlainString(),
                            deposit.getReferenceNumber() != null ? deposit.getReferenceNumber() : "N/A",
                            reason != null ? reason : "No reason provided",
                            deposit.getProcessedAt().format(formatter),
                            deposit.getOrganization().getCompanyName());

            emailService.sendEmail("noreply@papms.com", clientEmail, "Deposit Request Rejected", htmlBody);
        } catch (Exception e) {
            log.error("Failed to send rejection email: {}", e.getMessage());
        }
    }
}
