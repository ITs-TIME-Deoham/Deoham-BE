package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatMessage;
import com.deoham.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByRoomOrderBySentAtDesc(ChatRoom room, Pageable pageable);
}
