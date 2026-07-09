package com.deoham.user.repository;

import com.deoham.user.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByNickname(String nickname);

    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL, status = 'ACTIVE' WHERE id = :id AND deleted_at IS NOT NULL",
            nativeQuery = true)
    int reactivateIfDeleted(@Param("id") UUID id);

    @Query("SELECT u.hasSeenCardViewOnboarding FROM User u WHERE u.id = :userId")
    boolean hasSeenCardViewOnboarding(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE User u SET u.hasSeenCardViewOnboarding = true WHERE u.id = :userId")
    void markCardViewOnboardingSeen(@Param("userId") UUID userId);
}
