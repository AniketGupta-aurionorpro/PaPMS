package com.aurionpro.papms.service;

import com.aurionpro.papms.Enum.ConcernStatus;
import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.ConcernResponseDto;
import com.aurionpro.papms.dto.RaiseConcernRequest;
import com.aurionpro.papms.dto.UpdateConcernStatusRequest;
import com.aurionpro.papms.entity.*;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // IMPORT LOGGING
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j // ADDED FOR LOGGING
public class ConcernServiceImpl implements ConcernService {

    private final ConcernRepository concernRepository;
    private final AppUserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Logged-in user not found."));
    }

    @Override
    @Transactional
    public ConcernResponseDto raiseConcern(RaiseConcernRequest request) {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Employee profile not found for current user."));

        log.info("Employee '{}' (ID: {}) is raising a new concern with subject: '{}'",
                currentUser.getUsername(), employee.getId(), request.getSubject());

        Concern concern = Concern.builder()
                .employee(employee)
                .organization(employee.getOrganization())
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(ConcernStatus.OPEN)
                .build();

        Concern savedConcern = concernRepository.save(concern);
        log.info("Successfully saved new concern with ID: {}", savedConcern.getId());

        // Notify all ORG_ADMINs of the organization
        List<User> orgAdmins = userRepository.findByOrganizationIdAndRole(employee.getOrganization().getId(), Role.ORG_ADMIN);
        String message = "New concern raised by " + employee.getUser().getFullName() + ": '" + concern.getSubject() + "'";
        String link = "/admin/concerns/" + savedConcern.getId();
        orgAdmins.forEach(admin -> notificationService.createNotification(admin, message, link));
        log.info("Sent notifications to {} ORG_ADMINs for new concern ID: {}", orgAdmins.size(), savedConcern.getId());

        return toDto(savedConcern);
    }

    @Override
    @Transactional
    public ConcernResponseDto updateConcernStatus(Long concernId, UpdateConcernStatusRequest request) {
        User adminUser = getLoggedInUser();
        log.info("Admin '{}' is updating status of concern ID {} to {}", adminUser.getUsername(), concernId, request.getStatus());

        // This query correctly ensures the admin can only access concerns in their own organization
        Concern concern = concernRepository.findByIdAndOrganizationId(concernId, adminUser.getOrganizationId())
                .orElseThrow(() -> new NotFoundException("Concern not found or you don't have permission to access it."));

        // **ADDED VALIDATION LOGIC**
        if (request.getStatus() == ConcernStatus.RESOLVED &&
                (request.getResolutionNotes() == null || request.getResolutionNotes().isBlank())) {
            log.warn("Admin '{}' attempted to resolve concern {} without providing notes.", adminUser.getUsername(), concernId);
            throw new IllegalArgumentException("Resolution notes are required to resolve a concern.");
        }

        concern.setStatus(request.getStatus());
        concern.setResolutionNotes(request.getResolutionNotes()); // Save the admin's notes
        concern.setResolvedBy(adminUser);
        Concern updatedConcern = concernRepository.save(concern);
        log.info("Successfully updated concern ID {}. New status: {}, Notes added: {}",
                updatedConcern.getId(), updatedConcern.getStatus(), request.getResolutionNotes() != null);

        // Notify the employee about the update with more details
        User employeeUser = concern.getEmployee().getUser();
        String message = String.format("Your concern '%s' has been updated to %s. Admin Notes: %s",
                concern.getSubject(),
                request.getStatus().name(),
                request.getResolutionNotes() != null ? request.getResolutionNotes() : "N/A");
        String link = "/employee/concerns/" + concern.getId();
        notificationService.createNotification(employeeUser, message, link);

        return toDto(updatedConcern);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConcernResponseDto> getMyConcerns() {
        User currentUser = getLoggedInUser();
        Employee employee = employeeRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new NotFoundException("Employee profile not found."));
        return concernRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId())
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConcernResponseDto> getConcernsForOrganization(Integer organizationId) {
        User currentUser = getLoggedInUser();
        if (!currentUser.getOrganizationId().equals(organizationId)) {
            log.warn("SECURITY VIOLATION: User '{}' attempted to access concerns for organization ID {}",
                    currentUser.getUsername(), organizationId);
            throw new SecurityException("Access denied.");
        }
        return concernRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ConcernResponseDto getConcernById(Long concernId) {
        User currentUser = getLoggedInUser();
        Concern concern = concernRepository.findById(concernId)
                .orElseThrow(() -> new NotFoundException("Concern not found."));

        // Security Check
        boolean isEmployeeOwner = currentUser.getRole().equals(Role.EMPLOYEE) &&
                concern.getEmployee().getUser().getId().equals(currentUser.getId());

        boolean isAdminInOrg = currentUser.getRole().equals(Role.ORG_ADMIN) &&
                concern.getOrganization().getId().equals(currentUser.getOrganizationId());

        if (!isEmployeeOwner && !isAdminInOrg) {
            log.warn("SECURITY VIOLATION: User '{}' attempted to access unauthorized concern ID {}",
                    currentUser.getUsername(), concernId);
            throw new SecurityException("Access denied.");
        }

        return toDto(concern);
    }

    // This private mapper is updated to include the new field
    private ConcernResponseDto toDto(Concern concern) {
        return ConcernResponseDto.builder()
                .id(concern.getId())
                .subject(concern.getSubject())
                .description(concern.getDescription())
                .status(concern.getStatus().name())
                .createdAt(concern.getCreatedAt())
                .updatedAt(concern.getUpdatedAt())
                .resolutionNotes(concern.getResolutionNotes()) // ADDED THIS LINE
                .employeeId(concern.getEmployee().getId())
                .employeeName(concern.getEmployee().getUser().getFullName())
                .resolvedByAdminName(concern.getResolvedBy() != null ? concern.getResolvedBy().getFullName() : null)
                .build();
    }
}