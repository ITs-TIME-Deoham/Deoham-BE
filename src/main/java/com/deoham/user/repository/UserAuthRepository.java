package com.deoham.user.repository;

import com.deoham.user.entity.AuthProvider;
import com.deoham.user.entity.UserAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository extends JpaRepository<UserAuth, UUID> {

    Optional<UserAuth> findByProviderAndProviderUid(AuthProvider provider, String providerUid);

    Optional<UserAuth> findByUserIdAndProvider(UUID userId, AuthProvider provider);
}
