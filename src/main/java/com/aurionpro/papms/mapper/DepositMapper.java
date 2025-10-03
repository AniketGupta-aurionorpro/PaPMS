package com.aurionpro.papms.mapper;

import com.aurionpro.papms.dto.deposit.DepositResponse;
import com.aurionpro.papms.entity.Deposit;
import com.aurionpro.papms.entity.Transaction;

public class DepositMapper {


    public static DepositResponse toDto(Deposit deposit, Transaction transaction) {
        return DepositResponse.builder()
                .id(deposit.getId())
                .organizationId(deposit.getOrganization().getId())
                .organizationName(deposit.getOrganization().getCompanyName())
                .amountDeposited(deposit.getAmount())
                .balanceAfterDeposit(transaction.getBalanceAfterTransaction())
                .depositTimestamp(deposit.getDepositDate())
                .build();
    }
}