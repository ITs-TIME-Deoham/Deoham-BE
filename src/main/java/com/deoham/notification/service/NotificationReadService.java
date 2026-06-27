package com.deoham.notification.service;

import com.deoham.notification.dto.NotificationResponse;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationReadService {

    Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);
}
