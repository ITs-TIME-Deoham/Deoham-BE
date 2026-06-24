package com.deoham.user.repository;

import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.UserOauthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserOauthProviderRepository extends JpaRepository<UserOauthProvider, UUID> {

    Optional<UserOauthProvider> findByProviderAndProviderUserId(OauthProvider provider, String providerUserId);
}
