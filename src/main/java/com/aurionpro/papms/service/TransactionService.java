// service/TransactionService.java
package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.TransactionSourceType;
import com.aurionpro.papms.Enum.TransactionType;
import com.aurionpro.papms.dto.TransactionDto;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface TransactionService {
    Transaction processDebit(Organization organization, BigDecimal amount,
                             String description, TransactionSourceType sourceType, Long sourceId);
    Transaction processCredit(Organization organization, BigDecimal amount,
                              String description, TransactionSourceType sourceType, Long sourceId);
//   Page<TransactionDto> getTransactionsForOrganization(Integer organizationId, Pageable pageable);
    Page<TransactionDto> getTransactionsForOrganization(Integer organizationId, String searchTerm,
                                                    LocalDate startDate, LocalDate endDate,
                                                    TransactionType type, Pageable pageable);
}