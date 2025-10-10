package com.aurionpro.papms.config;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("DataSeeder running...");

        // Check if a BANK_ADMIN already exists to prevent duplicates
        if (appUserRepository.countUserByRoleEquals(Role.BANK_ADMIN) == 0) {
            log.info("No BANK_ADMIN found. Creating default BANK_ADMIN user.");

            // Create the default BANK_ADMIN user
            User bankAdmin = User.builder()
                    .username("Ani")
                    .password(passwordEncoder.encode("Ani")) // <-- IMPORTANT: Always encode passwords
                    .fullName("Ani")
                    .email("opaniketgupta@gmail.com")
                    .role(Role.BANK_ADMIN)
                    .organizationId(null) // BANK_ADMIN is not tied to an organization
                    .isActive(true)
                    .requiresPasswordChange(false) // <-- IMPORTANT: Set to false as requested
                    .build();

            appUserRepository.save(bankAdmin);
            log.info("Default BANK_ADMIN user 'Ani' created successfully.");
        } else {
            log.info("BANK_ADMIN user already exists. Skipping creation.");
        }
    }
}