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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class DepositServiceImpl implements DepositService {

    private final OrganizationRepository organizationRepository;
    private final DepositRepository depositRepository;
    private final AppUserRepository userRepository;
    private final TransactionService transactionService;

    @Override
    @Transactional
    public DepositResponse makeDepositForCurrentUser(DepositRequest depositRequest) {
        // currently logged-in user
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found."));

        //  b_admin
        Integer organizationId = currentUser.getOrganizationId();
        if (organizationId == null) {
            throw new IllegalStateException("User is not associated with any organization.");
        }


        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new NotFoundException("Organization not found with ID: " + organizationId));


        String description = "Organization self-service deposit.";
        Transaction transaction = transactionService.processCredit(
                organization,
                depositRequest.getAmount(),
                description,
                TransactionSourceType.MANUAL_ADJUSTMENT,
                currentUser.getId()
        );

        //  Create a specific audit record for this deposit event.
        Deposit deposit = Deposit.builder()
                .organization(organization)
                .amount(depositRequest.getAmount())
                .depositDate(LocalDateTime.now())
                .transactionId(transaction.getId())
                .build();

        Deposit savedDeposit = depositRepository.save(deposit);

        return DepositMapper.toDto(savedDeposit, transaction);
    }
}