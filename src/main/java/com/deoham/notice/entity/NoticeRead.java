package com.deoham.notice.entity;

import com.deoham.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "notice_reads")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeRead {

    @EmbeddedId
    private NoticeReadId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("noticeId")
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(name = "read_at", nullable = false, updatable = false)
    private Instant readAt;

    @Builder
    private NoticeRead(User user, Notice notice) {
        this.id = new NoticeReadId(user.getId(), notice.getId());
        this.user = user;
        this.notice = notice;
        this.readAt = Instant.now();
    }
}
