package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatMessage;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByChatRoomIdOrderBySentAtDesc(UUID chatRoomId, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndSentAtBeforeOrderBySentAtDesc(
            UUID chatRoomId, Instant before, Pageable pageable);

    List<ChatMessage> findByChatRoomIdAndSenderIdNotAndReadAtIsNull(UUID chatRoomId, UUID senderId);
}
