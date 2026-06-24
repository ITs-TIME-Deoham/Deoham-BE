package com.deoham.report.entity;

import com.deoham.user.entity.User;
import jakarta.persistence.Column;
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

import java.time.Instant;

@Getter
@Entity
@Table(name = "user_blocks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserBlock {

    @EmbeddedId
    private UserBlockId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("blockerId")
    @JoinColumn(name = "blocker_id")
    private User blocker;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("blockedId")
    @JoinColumn(name = "blocked_id")
    private User blocked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Builder
    private UserBlock(User blocker, User blocked) {
        this.id = new UserBlockId(blocker.getId(), blocked.getId());
        this.blocker = blocker;
        this.blocked = blocked;
        this.createdAt = Instant.now();
    }
}
