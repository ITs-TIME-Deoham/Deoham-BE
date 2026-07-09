package com.deoham.auth.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.client.KakaoUserInfo;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserSocialAccount;
import com.deoham.user.entity.UserStatus;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("카카오 재로그인 - 소프트 삭제 유저 복구")
class AuthServiceKakaoReactivationTest {

	private static final String KAKAO_ID = "123456789";

	@Autowired
	private AuthService authService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private UserSocialAccountRepository userSocialAccountRepository;

	@Autowired
	private OAuthStateRepository oauthStateRepository;

	@Autowired
	private EntityManager entityManager;

	@MockBean
	private KakaoOAuthClient kakaoOAuthClient;

	@Test
	@DisplayName("탈퇴한 유저가 같은 카카오 계정으로 재로그인하면 500 대신 계정이 복구된다")
	void kakaoLogin_reactivatesSoftDeletedUser() {
		stubKakao();

		// 최초 로그인 - 신규 유저 + 소셜 계정 생성
		KakaoCallbackResponse firstLogin = authService.kakaoLogin("code-1", seedState("state-1"));
		assertThat(firstLogin.isNewUser()).isTrue();

		UserSocialAccount socialAccount = userSocialAccountRepository
				.findByProviderAndProviderUid(OauthProvider.KAKAO, KAKAO_ID)
				.orElseThrow();
		var userId = socialAccount.getUserId();

		// 유저 탈퇴 (소프트 삭제)
		User user = userRepository.findById(userId).orElseThrow();
		user.delete();
		userRepository.saveAndFlush(user);

		// 영속성 컨텍스트를 비워서, 실제 운영 환경처럼 재로그인 요청이
		// 완전히 새 세션에서 lazy user 연관관계를 다시 조회하도록 강제한다.
		entityManager.flush();
		entityManager.clear();

		// 같은 카카오 계정으로 재로그인 -> 예외 없이 계정이 복구되어야 한다
		KakaoCallbackResponse secondLogin = authService.kakaoLogin("code-2", seedState("state-2"));
		assertThat(secondLogin.isNewUser()).isFalse();

		entityManager.clear();
		User reactivatedUser = userRepository.findById(userId).orElseThrow();
		assertThat(reactivatedUser.getDeletedAt()).isNull();
		assertThat(reactivatedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
	}

	private String seedState(String state) {
		oauthStateRepository.save(new OAuthState(state, OauthProvider.KAKAO, Instant.now().plusSeconds(300)));
		return state;
	}

	private void stubKakao() {
		when(kakaoOAuthClient.exchangeCode(anyString()))
				.thenReturn(new KakaoTokenResponse("bearer", "kakao-access-token", 3600L, "kakao-refresh-token", 3600L));
		when(kakaoOAuthClient.getUserInfo(anyString()))
				.thenReturn(new KakaoUserInfo(Long.parseLong(KAKAO_ID),
						new KakaoUserInfo.KakaoAccount("test@example.com", true, "male", "1995")));
	}
}
