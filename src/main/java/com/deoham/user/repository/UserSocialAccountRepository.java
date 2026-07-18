package com.deoham.user.repository;

import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.UserSocialAccount;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserSocialAccountRepository extends JpaRepository<UserSocialAccount, UUID> {

	@Query(value = "SELECT sa.id, sa.user_id, sa.provider, sa.provider_uid, sa.provider_email, sa.access_token, sa.refresh_token, sa.token_expires_at, sa.created_at, sa.updated_at FROM user_social_accounts sa WHERE sa.provider = CAST(:provider AS oauth_provider) AND sa.provider_uid = :providerUid", nativeQuery = true)
	Optional<UserSocialAccount> findByProviderAndProviderUid(@Param("provider") String provider, @Param("providerUid") String providerUid);

	Optional<UserSocialAccount> findByRefreshToken(String refreshToken);

	List<UserSocialAccount> findAllByUser_Id(UUID userId);

	void deleteAllByUser_Id(UUID userId);
}
