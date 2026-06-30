package com.deoham.notification.dto;

import com.deoham.notification.entity.Notification;
import com.deoham.notification.entity.NotifyType;
import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        NotifyType type,
        String message,
        boolean isRead,
        Instant createdAt,
        UUID referenceId
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt(),
                notification.getReferenceId()
        );
    }
}
