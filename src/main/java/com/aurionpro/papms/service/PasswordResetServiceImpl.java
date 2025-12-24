package com.aurionpro.papms.service;

import com.aurionpro.papms.dto.ForgotPasswordRequest;
import com.aurionpro.papms.dto.ResetPasswordRequest;
import com.aurionpro.papms.emails.EmailService;
import com.aurionpro.papms.entity.PasswordResetToken;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final AppUserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Override
    @Transactional
    public void handleForgotPasswordRequest(ForgotPasswordRequest request) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());

        if (userOptional.isEmpty()) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return; // Silently exit to prevent user enumeration attacks
        }

        User user = userOptional.get();

        // Delete any existing token for this user and flush immediately
        // This ensures the delete is executed before the insert
        tokenRepository.deleteByUser(user);
        tokenRepository.flush(); // Force the delete to execute immediately
        log.info("Cleared any existing password reset tokens for user '{}'", user.getUsername());

        // Now we can safely create a new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.saveAndFlush(resetToken); // Use saveAndFlush to ensure it's persisted

        // This link will now use the URL from your application.properties file
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;

        sendPasswordResetEmail(user, resetLink);
        log.info("New password reset link sent to email: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void handleResetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        PasswordResetToken token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new NotFoundException("Invalid or expired password reset token."));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new IllegalStateException("Password reset token has expired.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequiresPasswordChange(false);
        userRepository.save(user);

        tokenRepository.delete(token); // Invalidate the token after use
        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    private void sendPasswordResetEmail(User user, String resetLink) {
        String subject = "Your Password Reset Request for PaPMS";
        String body = String.format(
                """
                        <h3>Hello %s,</h3>
                        <p>You recently requested to reset your password for your PaPMS account. Click the link below to reset it.</p>
                        <p><a href="%s" style="color: #ffffff; background-color: #007bff; padding: 10px 15px; text-decoration: none; border-radius: 5px;">Reset Your Password</a></p>
                        <p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>
                        <p>This password reset link is only valid for the next 15 minutes.</p>
                        <br/>
                        <p>Thanks,</p>
                        <p>The PaPMS Team</p>
                        """,
                user.getFullName(), resetLink);

        emailService.sendEmail("no-reply@papms.com", user.getEmail(), subject, body);
    }
}