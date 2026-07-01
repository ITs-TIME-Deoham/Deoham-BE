package com.deoham.user.repository;

import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.UserSocialAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

    Optional<UserSocialAccount> findByProviderAndProviderUid(OauthProvider provider, String providerUid);

    Optional<UserSocialAccount> findByRefreshToken(String refreshToken);

    List<UserSocialAccount> findAllByUser_Id(UUID userId);
}
