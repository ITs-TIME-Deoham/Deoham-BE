package com.deoham.user.repository;

import com.deoham.user.entity.User;
import com.deoham.user.entity.UserStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByNickname(String nickname);

    @Query("SELECT u.hasSeenCardViewOnboarding FROM User u WHERE u.id = :userId")
    boolean hasSeenCardViewOnboarding(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.hasSeenCardViewOnboarding = true WHERE u.id = :userId")
    void markCardViewOnboardingSeen(@Param("userId") UUID userId);

    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt < :deletedBefore")
    List<User> findByStatusAndDeletedAtBefore(@Param("status") UserStatus status, @Param("deletedBefore") Instant deletedBefore);
}
