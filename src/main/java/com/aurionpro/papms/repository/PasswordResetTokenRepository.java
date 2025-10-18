package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.PasswordResetToken;
import com.aurionpro.papms.entity.User; // ADD THIS IMPORT
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    void deleteAllByExpiryDateBefore(LocalDateTime now);

    void deleteByUser(User user);
}