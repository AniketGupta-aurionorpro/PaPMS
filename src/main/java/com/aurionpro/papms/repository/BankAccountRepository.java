// repository/BankAccountRepository.java
package com.aurionpro.papms.repository;

import com.aurionpro.papms.Enum.OwnerType;
import com.aurionpro.papms.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    Optional<BankAccount> findByOwnerIdAndOwnerTypeAndIsPrimaryTrue(Long ownerId, OwnerType ownerType);
}