package com.deoham.user.entity;

import com.deoham.global.entity.BaseEntity;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Getter
@Entity
@Table(
        name = "user_auth",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "user_auth_provider_uid_unique",
                        columnNames = {"provider", "provider_uid"}
                ),
                @UniqueConstraint(
                        name = "user_auth_user_provider_unique",
                        columnNames = {"user_id", "provider"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuth extends BaseEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    private AuthProvider provider;

    @Column(name = "provider_uid", length = 255)
    private String providerUid;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Builder
    private UserAuth(User user, AuthProvider provider, String providerUid, String passwordHash) {
        this.user = user;
        this.provider = provider;
        this.providerUid = providerUid;
        this.passwordHash = passwordHash;
    }
}
