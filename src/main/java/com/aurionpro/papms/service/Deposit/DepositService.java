package com.aurionpro.papms.service.Deposit;

import com.aurionpro.papms.dto.deposit.DepositRequest;
import com.aurionpro.papms.dto.deposit.DepositResponse;


public interface DepositService {
    //DepositResponse makeDeposit(Integer organizationId, DepositRequest depositRequest);

    /**
     * Processes a deposit for the currently authenticated ORG_ADMIN.
     * The organization is inferred from the user's security context.
     *
     * @param depositRequest DTO containing the amount to deposit.
     * @return A DTO with the details of the completed deposit.
     */
    DepositResponse makeDepositForCurrentUser(DepositRequest depositRequest);
}