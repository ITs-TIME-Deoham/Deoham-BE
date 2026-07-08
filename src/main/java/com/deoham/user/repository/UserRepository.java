package com.deoham.user.repository;

import com.deoham.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByFirebaseUid(String firebaseUid);

    boolean existsByNickname(String nickname);

    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query("SELECT u FROM User u WHERE u.status = 'DELETED' AND u.deletedAt IS NOT NULL AND u.deletedAt < :deletedBefore")
    List<User> findDeletedUsersBefore(@Param("deletedBefore") Instant deletedBefore);
}
