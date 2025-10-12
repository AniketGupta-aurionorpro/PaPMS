package com.aurionpro.papms.dto;

import com.aurionpro.papms.entity.Notification;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationDto {
    private Long id;
    private String message;
    private boolean isRead;
    private String link;
    private LocalDateTime createdAt;

    public static NotificationDto fromEntity(Notification entity) {
        return NotificationDto.builder()
                .id(entity.getId())
                .message(entity.getMessage())
                .isRead(entity.isRead())
                .link(entity.getLink())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}