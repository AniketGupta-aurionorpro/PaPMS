package com.aurionpro.papms.emails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Send email with custom from address (for different organizations or the bank)
    public void sendEmail(String from, String to, String subject, String body) {
        log.info("Preparing to send email from '{}' to '{}' with subject: {}", from, to, subject);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true means HTML content
        };
        try {
            mailSender.send(messagePreparator);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            // Depending on requirements, you might want to re-throw this as a custom exception
        }
    }

    // Send email to multiple employees (e.g., salary notification)
    public void sendEmailsToMultiple(List<String> toEmails, String from, String subject, String body) {
        log.info("Preparing to send bulk email to {} recipients with subject: {}", toEmails.size(), subject);
        for (String to : toEmails) {
            sendEmail(from, to, subject, body);
        }
    }
}