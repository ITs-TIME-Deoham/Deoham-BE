package com.deoham.notification.entity;

import com.deoham.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "fcm_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class FcmToken {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, columnDefinition = "TEXT")
    private String token;

    @Column(name = "device_id")
    private String deviceId;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "platform", columnDefinition = "fcm_platform")
    private Platform platform;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Builder
    private FcmToken(User user, String token, String deviceId, Platform platform) {
        this.user = user;
        this.token = token;
        this.deviceId = deviceId;
        this.platform = platform;
    }

    /** 같은 기기에서 FCM 토큰이 갱신(rotation)됐을 때 토큰 값만 교체한다. */
    public void updateToken(String token, Platform platform) {
        this.token = token;
        this.platform = platform;
    }

    /** 같은 토큰이 다른 유저 로그인으로 재사용될 때(계정 전환) 소유자와 기기 정보를 재할당한다. */
    public void reassignTo(User user, String deviceId, Platform platform) {
        this.user = user;
        this.deviceId = deviceId;
        this.platform = platform;
    }
}
