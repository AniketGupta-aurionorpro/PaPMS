// service/TransactionService.java
package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import java.math.BigDecimal;

public interface TransactionService {
    Transaction processDebit(Organization organization, BigDecimal amount,
                             String description, TransactionSourceType sourceType, Long sourceId);
    Transaction processCredit(Organization organization, BigDecimal amount,
                              String description, TransactionSourceType sourceType, Long sourceId);
}