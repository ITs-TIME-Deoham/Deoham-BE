package com.deoham.chat.repository;

import com.deoham.chat.entity.ChatRoomMember;
import com.deoham.chat.entity.ChatRoomMemberId;
import com.deoham.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, ChatRoomMemberId> {

    List<ChatRoomMember> findByUser(User user);
}
