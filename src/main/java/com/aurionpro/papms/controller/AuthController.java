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
		if (userRepo.existsByUsername(req.username())) {
			return ResponseEntity.status(HttpStatus.CONFLICT).body("Username already exists");
		}
        if(req.role() == Role.BANK_ADMIN) {
            Long count = userRepo.countUserByRoleEquals(Role.BANK_ADMIN);
            if(count > 0) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only one BANK_ADMIN allowed");
            }
            User u = User.builder().username(req.username()).password(encoder.encode(req.password())).fullName(req.fullName()).email(req.email()).role(req.role()).organizationId(null).isActive(true).build();
            userRepo.save(u);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
		User u = User.builder().username(req.username()).password(encoder.encode(req.password())).fullName(req.fullName()).email(req.email()).role(req.role()).organizationId(req.organizationId()).isActive(true).build();
		userRepo.save(u);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

//	@PostMapping("/login")
//	public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req) {
//		Authentication auth = new UsernamePasswordAuthenticationToken(req.username(), req.password());
//		authManager.authenticate(auth);
//		var user = uds.loadUserByUsername(req.username());
//		String token = jwtService.generateToken(user, jwtExp);
//		return ResponseEntity.ok(new AuthResponse(token));
//	}
@PostMapping("/login")
public ResponseEntity<LoginResponseDto> login(@RequestBody AuthRequest req) {
    // Step 1: Authenticate the user
    Authentication auth = new UsernamePasswordAuthenticationToken(req.username(), req.password());
    authManager.authenticate(auth);

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
            // For org admins and clients, add organization details
            if (user.getOrganizationId() != null) {
                organizationRepository.findById(user.getOrganizationId()).ifPresent(org -> {
                    responseBuilder.organizationId(org.getId());
                    responseBuilder.organizationName(org.getCompanyName());
                });
            }
            break;

        case BANK_ADMIN:
            // No additional details needed for Bank Admin
            break;
    }

    return ResponseEntity.ok(responseBuilder.build());
}
    @PostMapping("/force-change-password")
    public ResponseEntity<String> forceChangePassword(@Valid @RequestBody ForceChangePasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            return ResponseEntity.badRequest().body("New password and confirmation do not match.");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));

        if (!user.getRequiresPasswordChange()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("This endpoint is only for initial password change.");
        }

        user.setPassword(encoder.encode(request.getNewPassword()));
        user.setRequiresPasswordChange(false);
        userRepo.save(user);

        return ResponseEntity.ok("Password has been changed successfully. Please log in again.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.handleForgotPasswordRequest(request);
        // SECURITY: Always return a generic success message to prevent user enumeration attacks.
        return ResponseEntity.ok("If an account with this email exists, a password reset link has been sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.handleResetPassword(request);
        return ResponseEntity.ok("Password has been reset successfully.");
    }
}
