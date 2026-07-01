package com.deoham.auth.repository;

import com.deoham.auth.entity.OAuthState;
import com.deoham.user.entity.OauthProvider;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface OAuthStateRepository extends JpaRepository<OAuthState, String> {

    Optional<OAuthState> findByStateAndProvider(String state, OauthProvider provider);

    @Modifying
    long deleteByExpiresAtBefore(Instant expiresAt);
}
