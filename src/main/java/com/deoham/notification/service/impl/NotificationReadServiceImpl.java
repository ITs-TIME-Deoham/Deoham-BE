package com.deoham.notification.service.impl;

import com.deoham.notification.repository.NotificationRepository;
import com.deoham.notification.service.NotificationReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationReadServiceImpl implements NotificationReadService {

    private final NotificationRepository notificationRepository;
}
