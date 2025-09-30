// repository/TransactionRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}