package com.aurionpro.papms.batch;

import com.aurionpro.papms.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupTask {

    private final PasswordResetTokenRepository tokenRepository;

    /**
     * This task runs every day at 3:00 AM server time to purge expired password reset tokens.
     * The cron expression is "seconds minutes hours day-of-month month day-of-week".
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void purgeExpiredTokens() {
        log.info("Running scheduled task to purge expired password reset tokens.");
        try {
            tokenRepository.deleteAllByExpiryDateBefore(LocalDateTime.now());
            log.info("Successfully purged expired tokens.");
        } catch (Exception e) {
            log.error("Error during expired token cleanup task", e);
        }
    }
}