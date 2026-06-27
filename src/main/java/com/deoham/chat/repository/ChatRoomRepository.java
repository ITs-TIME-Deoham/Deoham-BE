package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatRoom;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("""
            SELECT m1.chatRoom FROM ChatRoomMember m1
            JOIN ChatRoomMember m2 ON m1.chatRoom = m2.chatRoom
            WHERE m1.chatRoom.isDirect = true
              AND m1.user.id = :userId1 AND m2.user.id = :userId2
            """)
    Optional<ChatRoom> findDirectRoomBetween(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);

    @Query("""
            SELECT m.chatRoom FROM ChatRoomMember m
            WHERE m.user.id = :userId AND m.leftAt IS NULL
            ORDER BY m.chatRoom.lastMessageAt DESC NULLS LAST
            """)
    Page<ChatRoom> findMyRooms(@Param("userId") UUID userId, Pageable pageable);
}
