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
        Optional<User> userOptional = userRepository.findByUsername(request.getEmail());

        if (userOptional.isEmpty()) {
            // SECURITY: Do not reveal if the user exists.
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            return; // Silently exit
        }

        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken(token, user);
        tokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + token;

        sendPasswordResetEmail(user, resetLink);
        log.info("Password reset link sent to email: {}", user.getEmail());
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
        user.setRequiresPasswordChange(false); // The password is now changed
        userRepository.save(user);

        tokenRepository.delete(token); // Invalidate the token after use
        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    private void sendPasswordResetEmail(User user, String resetLink) {
        String subject = "Your Password Reset Request for PaPMS";
        String body = String.format("""
            <h3>Hello %s,</h3>
            <p>You recently requested to reset your password for your PaPMS account. Click the link below to reset it.</p>
            <p><a href="%s" style="color: #ffffff; background-color: #007bff; padding: 10px 15px; text-decoration: none; border-radius: 5px;">Reset Your Password</a></p>
            <p>If you did not request a password reset, please ignore this email or contact support if you have concerns.</p>
            <p>This password reset link is only valid for the next 15 minutes.</p>
            <br/>
            <p>Thanks,</p>
            <p>The PaPMS Team</p>
            """, user.getFullName(), resetLink);

        emailService.sendEmail("no-reply@papms.com", user.getEmail(), subject, body);
    }
}