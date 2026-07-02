package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatRoomIdOrderBySentAtDesc(UUID chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndSentAtBeforeOrderBySentAtDesc(
            UUID chatRoomId, Instant before, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndSenderIdNotAndReadAtIsNull(UUID chatRoomId, UUID senderId);

    long countByChatRoomIdAndSenderIdNotAndReadAtIsNull(UUID chatRoomId, UUID senderId);

    @Query("""
            SELECT m.chatRoom.id AS roomId, COUNT(m) AS unreadCount
            FROM ChatMessage m
            WHERE m.chatRoom.id IN :roomIds
              AND m.sender.id <> :userId
              AND m.readAt IS NULL
            GROUP BY m.chatRoom.id
            """)
    List<UnreadCountProjection> countUnreadGroupedByRoom(
            @Param("roomIds") List<UUID> roomIds, @Param("userId") UUID userId);
}
