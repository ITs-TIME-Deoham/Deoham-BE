package com.deoham.chat.entity;

import com.deoham.user.entity.User;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_room_members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember {

    @EmbeddedId
    private ChatRoomMemberId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("roomId")
    @JoinColumn(name = "room_id")
    private ChatRoom room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    private ChatRoomMember(ChatRoom room, User user) {
        this.id = new ChatRoomMemberId(room.getId(), user.getId());
        this.room = room;
        this.user = user;
    }
}
