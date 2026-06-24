package com.deoham.notification.repository;

import com.deoham.notification.entity.Notification;
import com.deoham.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
