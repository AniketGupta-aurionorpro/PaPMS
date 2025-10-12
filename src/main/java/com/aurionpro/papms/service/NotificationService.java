package com.aurionpro.papms.service;

import com.aurionpro.papms.entity.Notification;
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    /**
     * Creates and saves a new notification for a specific user.
     *
     * @param user    The recipient of the notification.
     * @param message The content of the notification.
     * @param link    An optional frontend link for navigation.
     */
    @Transactional
    public void createNotification(User user, String message, String link) {
        if (user == null) {
            log.warn("Attempted to create a notification for a null user.");
            return;
        }

        Notification notification = Notification.builder()
                .user(user)
                .message(message)
                .link(link)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
        log.info("Created notification for user '{}': {}", user.getUsername(), message);
    }
}