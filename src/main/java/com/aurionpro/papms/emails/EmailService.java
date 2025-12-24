package com.aurionpro.papms.emails;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.mail.display-name:PaPMS Support}")
    private String displayName;

    // Send email - shows display name to recipients (e.g., "PaPMS Support <email>")
    @Async
    public void sendEmail(String from, String to, String subject, String body) {
        log.info("Preparing to send email to '{}' with subject: {}", to, subject);
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            // Use display name so recipients see "PaPMS Support" instead of personal email
            helper.setFrom(new InternetAddress(senderEmail, displayName));
            helper.setReplyTo(from); // Replies go to the requested "from" address
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true means HTML content
        };
        try {
            mailSender.send(messagePreparator);
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}. Error: {}", to, e.getMessage());
            // Don't throw - let the request complete successfully even if email fails
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