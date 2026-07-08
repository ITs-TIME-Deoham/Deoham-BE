package com.deoham.notification.repository;

import com.deoham.notification.entity.FcmToken;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {

    List<FcmToken> findByUser(User user);
}
