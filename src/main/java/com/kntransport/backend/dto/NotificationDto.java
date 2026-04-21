package com.kntransport.backend.dto;

import com.kntransport.backend.entity.Notification;

public record NotificationDto(
        String id,
        String type,
        String title,
        String body,
        String timestamp,
        boolean read
) {
    public static NotificationDto from(Notification n) {
        return new NotificationDto(
                n.getId().toString(),
                n.getType().name(),
                n.getTitle(),
                n.getBody(),
                n.getTimestamp().toString(),
                n.isRead()
        );
    }
}
