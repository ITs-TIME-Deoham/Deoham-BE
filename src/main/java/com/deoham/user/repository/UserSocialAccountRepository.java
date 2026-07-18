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

	@Query("SELECT sa FROM UserSocialAccount sa LEFT JOIN FETCH sa.user WHERE sa.provider = :provider AND sa.providerUid = :providerUid")
	Optional<UserSocialAccount> findByProviderAndProviderUid(@Param("provider") OauthProvider provider, @Param("providerUid") String providerUid);

	Optional<UserSocialAccount> findByRefreshToken(String refreshToken);

	List<UserSocialAccount> findAllByUser_Id(UUID userId);

	void deleteAllByUser_Id(UUID userId);
}
