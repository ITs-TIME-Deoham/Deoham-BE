package com.deoham.chat.entity;

import com.deoham.global.entity.BaseEntity;
import com.deoham.project.entity.Project;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "chat_room")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "is_direct", nullable = false)
    private boolean isDirect;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Builder
    private ChatRoom(Project project, String name, boolean isDirect) {
        this.project = project;
        this.name = name;
        this.isDirect = isDirect;
    }

    public void touchLastMessageAt(Instant at) {
        this.lastMessageAt = at;
    }
}
