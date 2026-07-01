package com.deoham.auth.entity;

import com.deoham.user.entity.OauthProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "oauth_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OAuthState {

    @Id
    @Column(name = "state", nullable = false, updatable = false, length = 128)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, updatable = false, length = 20)
    private OauthProvider provider;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public OAuthState(String state, OauthProvider provider, Instant expiresAt) {
        this.state = state;
        this.provider = provider;
        this.expiresAt = expiresAt;
        this.createdAt = Instant.now();
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
