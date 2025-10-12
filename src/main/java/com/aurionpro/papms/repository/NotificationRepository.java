package com.aurionpro.papms.repository;

import com.aurionpro.papms.entity.Notification;
import com.aurionpro.papms.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Find all notifications for a user, ordered by most recent
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Count unread notifications for a user (for the bell icon badge)
    long countByUserAndIsReadFalse(User user);

    // Find all unread notifications for a user
    List<Notification> findByUserAndIsReadFalse(User user);
}