package com.aurionpro.papms.service.Deposit;

import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.dto.deposit.DepositRequest;
import com.aurionpro.papms.dto.deposit.DepositResponse;
import com.aurionpro.papms.entity.Deposit;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.mapper.DepositMapper;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.DepositRepository;
import com.aurionpro.papms.repository.OrganizationRepository;
import com.aurionpro.papms.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aurionpro.papms.service.NotificationService;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepositServiceImpl implements DepositService {

    private final OrganizationRepository organizationRepository;
    private final DepositRepository depositRepository;
    private final AppUserRepository userRepository;
    private final TransactionService transactionService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public DepositResponse makeDepositForCurrentUser(DepositRequest depositRequest) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        Integer organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            throw new IllegalStateException("User is not associated with any organization.");
        }
        log.info("Processing self-deposit of {} for user {} in organization ID {}", depositRequest.getAmount(), username, organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));

        // --- MODIFICATION START ---
        // 1. Create and save the Deposit record first to generate its ID.
        //    The transactionId will be null initially.
        Deposit deposit = Deposit.builder()
                .organization(organization)
                .amount(depositRequest.getAmount())
                .depositDate(LocalDateTime.now())
                .build();
        Deposit savedDeposit = depositRepository.save(deposit);
        log.info("Deposit record {} created. Now creating associated transaction.", savedDeposit.getId());

        // 2. Now, create the credit transaction using the NEW deposit ID as the sourceId.
        String description = "Organization self-service deposit.";
        Transaction transaction = transactionService.processCredit(
                organization,
                depositRequest.getAmount(),
                description,
                TransactionSourceType.DEPOSIT,
                savedDeposit.getId() // Use the correct source ID
        );
        log.info("Credit transaction {} created for deposit.", transaction.getId());

        // 3. Link the transaction back to the deposit record for a two-way relationship.
        savedDeposit.setTransactionId(transaction.getId());
        depositRepository.save(savedDeposit);
        log.info("Deposit record {} updated with transaction ID {}.", savedDeposit.getId(), transaction.getId());

        // 4. Send notification and prepare the response DTO.
        String message = String.format("Your deposit of ₹%.2f was successful. Your new balance is ₹%.2f.",
                savedDeposit.getAmount(), transaction.getBalanceAfterTransaction());
        notificationService.createNotification(currentUser, message, null);

        return DepositMapper.toDto(savedDeposit, transaction);
        // --- MODIFICATION END ---
    }
}