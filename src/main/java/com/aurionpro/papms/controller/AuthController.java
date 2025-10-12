package com.aurionpro.papms.controller;

import java.util.Set;
import java.util.stream.Collectors;

import com.aurionpro.papms.Enum.Role;
import com.aurionpro.papms.dto.*;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.security.jwt.JwtService;
import jakarta.validation.Valid;
import com.aurionpro.papms.service.PasswordResetService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.aurionpro.papms.service.EmployeeService;
import com.aurionpro.papms.entity.Organization;
import com.aurionpro.papms.repository.OrganizationRepository;


import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {
    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserDetailsService uds;
    private final PasswordEncoder encoder;
    private final AppUserRepository userRepo;
    private final PasswordResetService passwordResetService;
    private final EmployeeService employeeService; // INJECT EMPLOYEE SERVICE
    private final OrganizationRepository organizationRepository;

    @Value("${app.jwt.expiration}")
    private long jwtExp;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        log.info("Received registration request for username: {}", req.username());
        if (userRepo.existsByUsername(req.username())) {
            log.warn("Registration failed: Username {} already exists.", req.username());
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
        }
        if(req.role() == Role.BANK_ADMIN) {
            Long count = userRepo.countUserByRoleEquals(Role.BANK_ADMIN);
            if(count > 0) {
                log.warn("Registration for BANK_ADMIN failed: A BANK_ADMIN already exists.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only one BANK_ADMIN allowed");
            }
            User u = User.builder().username(req.username()).password(encoder.encode(req.password())).fullName(req.fullName()).email(req.email()).role(req.role()).organizationId(null).isActive(true).build();
            userRepo.save(u);
            log.info("Successfully registered BANK_ADMIN user: {}", req.username());
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        User u = User.builder().username(req.username()).password(encoder.encode(req.password())).fullName(req.fullName()).email(req.email()).role(req.role()).organizationId(req.organizationId()).isActive(true).build();
        userRepo.save(u);
        log.info("Successfully registered user: {} with role: {}", req.username(), req.role());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody AuthRequest req) {
        log.info("Login attempt for user: {}", req.username());
        // Step 1: Authenticate the user
        Authentication auth = new UsernamePasswordAuthenticationToken(req.username(), req.password());
        authManager.authenticate(auth);
        log.info("User {} authenticated successfully.", req.username());

        // Step 2: Load UserDetails and generate JWT
        UserDetails userDetails = uds.loadUserByUsername(req.username());
        String token = jwtService.generateToken(userDetails, jwtExp);

        // Step 3: Fetch the full User entity to get role and other details
        User user = userRepo.findByUsername(req.username())
                .orElseThrow(() -> new NotFoundException("User not found after successful authentication."));

        // Step 4: Build the comprehensive LoginResponseDto
        LoginResponseDto.LoginResponseDtoBuilder responseBuilder = LoginResponseDto.builder()
                .accessToken(token)
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole());

        // Step 5: Add role-specific details
        switch (user.getRole()) {
            case EMPLOYEE:
                log.debug("Fetching employee profile for user: {}", user.getUsername());
                // For employees, fetch and attach the complete profile
                CompleteEmployeeResponse employeeProfile = employeeService.getCompleteEmployeeProfileByUsername(user.getUsername());
                responseBuilder.employeeProfile(employeeProfile);
                // Also add organization details
                if (user.getOrganizationId() != null) {
                    organizationRepository.findById(user.getOrganizationId()).ifPresent(org -> {
                        responseBuilder.organizationId(org.getId());
                        responseBuilder.organizationName(org.getCompanyName());
                    });
                }
                break;

            case ORG_ADMIN:
            case CLIENT:
                log.debug("Fetching organization details for user: {}", user.getUsername());
                // For org admins and clients, add organization details
                if (user.getOrganizationId() != null) {
                    organizationRepository.findById(user.getOrganizationId()).ifPresent(org -> {
                        responseBuilder.organizationId(org.getId());
                        responseBuilder.organizationName(org.getCompanyName());
                    });
                }
                break;

            case BANK_ADMIN:
                log.debug("User is BANK_ADMIN, no extra details needed.");
                // No additional details needed for Bank Admin
                break;
        }
        log.info("Login successful for user: {}. Role: {}", user.getUsername(), user.getRole());
        return ResponseEntity.ok(responseBuilder.build());
    }
    @PostMapping("/force-change-password")
    public ResponseEntity<String> forceChangePassword(@Valid @RequestBody ForceChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        log.info("Received force password change request for user: {}", username);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            log.warn("Force password change for user {} failed: Passwords do not match.", username);
            return ResponseEntity.badRequest().body("New password and confirmation do not match.");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getRequiresPasswordChange()) {
            log.warn("Force password change for user {} rejected: Not required.", username);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This endpoint is only for initial password change.");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setRequiresPasswordChange(false);
        userRepo.save(user);

        log.info("Password successfully changed for user: {}. User must now log in again.", username);
        return ResponseEntity.ok("Password has been changed successfully. Please log in again.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Received forgot password request for email: {}", request.getEmail());
        passwordResetService.handleForgotPasswordRequest(request);
        // SECURITY: Always return a generic success message to prevent user enumeration attacks.
        log.info("Forgot password process initiated for email: {}. No confirmation of existence provided to client.", request.getEmail());
        return ResponseEntity.ok("If an account with this email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        log.info("Received password reset request with token.");
        passwordResetService.handleResetPassword(request);
        log.info("Password reset successful for token.");
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}