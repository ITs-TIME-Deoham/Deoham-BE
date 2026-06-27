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
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private ChatMessageType messageType;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "attachment_url", columnDefinition = "TEXT")
    private String attachmentUrl;

    @Column(name = "attachment_file_name", length = 255)
    private String attachmentFileName;

    @Column(name = "attachment_content_type", length = 100)
    private String attachmentContentType;

    @Column(name = "attachment_size_bytes")
    private Long attachmentSizeBytes;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Builder
    private ChatMessage(ChatRoom chatRoom, User sender, ChatMessageType messageType,
                         String content, String attachmentUrl, String attachmentFileName,
                         String attachmentContentType, Long attachmentSizeBytes) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.messageType = messageType;
        this.content = content;
        this.attachmentUrl = attachmentUrl;
        this.attachmentFileName = attachmentFileName;
        this.attachmentContentType = attachmentContentType;
        this.attachmentSizeBytes = attachmentSizeBytes;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
