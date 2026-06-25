package com.deoham.chat.entity;

import com.deoham.global.entity.BaseEntity;
import com.deoham.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(
        name = "chat_room_member",
        uniqueConstraints = @UniqueConstraint(
                name = "chat_room_member_room_user_unique",
                columnNames = {"chat_room_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomMember extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private ChatRoomMemberRole role;

    @Column(name = "last_read_message_at")
    private Instant lastReadMessageAt;

    @Column(name = "left_at")
    private Instant leftAt;

    @Builder
    private ChatRoomMember(ChatRoom chatRoom, User user, ChatRoomMemberRole role) {
        this.chatRoom = chatRoom;
        this.user = user;
        this.role = role == null ? ChatRoomMemberRole.MEMBER : role;
    }

    public void markReadUpTo(Instant at) {
        this.lastReadMessageAt = at;
    }

    public void leave(Instant at) {
        this.leftAt = at;
    }

    public boolean isActive() {
        return leftAt == null;
    }
}
