package com.deoham.notification.service.impl;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.notification.dto.NotificationResponse;
import com.deoham.notification.entity.Notification;
import com.deoham.notification.repository.NotificationRepository;
import com.deoham.notification.service.NotificationReadService;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReadServiceImpl implements NotificationReadService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public Page<NotificationResponse> getNotifications(UUID userId, Pageable pageable) {
        User user = getUser(userId);
        return notificationRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::from);
    }

    @Override
    @Transactional
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "알림을 찾을 수 없습니다"));
        if (!notification.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        notification.markAsRead();
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        User user = getUser(userId);
        notificationRepository.markAllAsReadByUser(user);
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다"));
    }
}
