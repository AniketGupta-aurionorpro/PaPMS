package com.aurionpro.papms.controller;

import com.aurionpro.papms.dto.NotificationDto; // We need to create this DTO
import com.aurionpro.papms.entity.User;
import com.aurionpro.papms.exception.NotFoundException;
import com.aurionpro.papms.repository.AppUserRepository;
import com.aurionpro.papms.repository.NotificationRepository;
import com.aurionpro.papms.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    private User getLoggedInUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Logged-in user not found."));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all notifications for the current user (paginated)")
    public ResponseEntity<Page<NotificationDto>> getMyNotifications(@ParameterObject Pageable pageable) {
        User currentUser = getLoggedInUser();
        log.info("Fetching notifications for user: {} with pagination: {}", currentUser.getUsername(), pageable);
        Page<Notification> notificationPage = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        log.info("Returning {} notifications for user {}", notificationPage.getTotalElements(), currentUser.getUsername());
        return ResponseEntity.ok(notificationPage.map(NotificationDto::fromEntity));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the count of unread notifications for the current user")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        User currentUser = getLoggedInUser();
        log.debug("Fetching unread notification count for user: {}", currentUser.getUsername());
        long count = notificationRepository.countByUserAndIsReadFalse(currentUser);
        log.debug("Found {} unread notifications for user {}", count, currentUser.getUsername());
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }

    @PutMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mark a specific notification as read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        User currentUser = getLoggedInUser();
        log.info("User {} requests to mark notification ID {} as read", currentUser.getUsername(), notificationId);
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification not found."));

        if (!notification.getUser().getId().equals(currentUser.getId())) {
            log.warn("SECURITY ALERT: User {} attempted to mark notification ID {} belonging to another user as read.", currentUser.getUsername(), notificationId);
            throw new SecurityException("You can only mark your own notifications as read.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
        log.info("Notification ID {} successfully marked as read.", notificationId);
        return ResponseEntity.ok().build();
    }
}