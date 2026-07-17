package com.deoham.notification.repository;

import com.deoham.notification.entity.FcmToken;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {

    List<FcmToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM FcmToken f WHERE f.user.id = :userId")
    void deleteAllByUser_Id(@Param("userId") UUID userId);
}
