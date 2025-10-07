// security/CsvUploadSecurityValidator.java
package com.aurionpro.papms.security;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CsvUploadSecurityValidator {

    private final AppUserRepository appUserRepository;

    public void validateOrganizationAccess(Integer organizationId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User currentUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new SecurityException("User not found"));

        if (currentUser.getRole() != Role.ORG_ADMIN ||
                !currentUser.getOrganizationId().equals(organizationId)) {
            throw new SecurityException("Access denied to organization: " + organizationId);
        }
    }

    public void validateCsvUploadLimit(Integer organizationId, int recordCount) {
        // Implement rate limiting or quota checking
        if (recordCount > 1000) {
            throw new SecurityException("CSV upload limit exceeded. Maximum 1000 records allowed.");
        }
    }
}