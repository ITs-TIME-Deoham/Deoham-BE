package com.deoham.notification.dto;

import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotificationType type,
        String message,
        boolean isRead,
        Instant createdAt,
        UUID referenceId
) {
    public static NotificationResponse from(Notification notification) {
        UUID referenceId = notification.getChatMessage() != null
                ? notification.getChatMessage().getId()
                : null;
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                referenceId
        );
    }
}
