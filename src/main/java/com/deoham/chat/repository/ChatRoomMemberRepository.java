package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatRoomMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, UUID> {

    Optional<ChatRoomMember> findByChatRoomIdAndUserIdAndLeftAtIsNull(UUID chatRoomId, UUID userId);

    boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(UUID chatRoomId, UUID userId);

    List<ChatRoomMember> findByChatRoomIdAndLeftAtIsNull(UUID chatRoomId);
}
