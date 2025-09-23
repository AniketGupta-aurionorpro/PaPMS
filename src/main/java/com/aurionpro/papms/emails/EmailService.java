package com.aurionpro.papms.emails;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.List;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    // Send email with custom from address (for different organizations or the bank)
    public void sendEmail(String from, String to, String subject, String body) {
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true); // true means HTML content
        };
        mailSender.send(messagePreparator);
    }

    // Send email to multiple employees (e.g., salary notification)
    public void sendEmailsToMultiple(List<String> toEmails, String from, String subject, String body) {
        for (String to : toEmails) {
            sendEmail(from, to, subject, body);
        }
    }
}