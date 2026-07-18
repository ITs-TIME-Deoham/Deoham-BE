package com.deoham.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.client.KakaoUserInfo;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.security.JwtProperties;
import com.deoham.global.security.JwtTokenProvider;
import com.deoham.user.entity.GenderType;
import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserSocialAccount;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 카카오 로그인 온보딩 신호 테스트")
class AuthServiceOnboardingTest {

	private static final String CODE = "auth-code";
	private static final String STATE = "state-value";
	private static final Long KAKAO_ID = 123456789L;

	@Mock
	private KakaoOAuthClient kakaoOAuthClient;
	@Mock
	private KakaoOAuthProperties kakaoOAuthProperties;
	@Mock
	private JwtTokenProvider jwtTokenProvider;
	@Mock
	private JwtProperties jwtProperties;
	@Mock
	private UserRepository userRepository;
	@Mock
	private UserSocialAccountRepository userSocialAccountRepository;
	@Mock
	private OAuthStateRepository oauthStateRepository;

	private AuthService authService;

	@BeforeEach
	void setUp() {
		authService = new AuthService(
				kakaoOAuthClient,
				kakaoOAuthProperties,
				jwtTokenProvider,
				jwtProperties,
				userRepository,
				userSocialAccountRepository,
				oauthStateRepository);

		when(oauthStateRepository.findByStateAndProvider(STATE, OauthProvider.KAKAO))
				.thenReturn(Optional.of(new OAuthState(
						STATE, OauthProvider.KAKAO, Instant.now().plusSeconds(300))));
		when(kakaoOAuthClient.exchangeCode(CODE))
				.thenReturn(new KakaoTokenResponse("bearer", "kakao-access", 3600, "kakao-refresh", 3600));
		when(kakaoOAuthClient.getUserInfo("kakao-access"))
				.thenReturn(new KakaoUserInfo(KAKAO_ID,
						new KakaoUserInfo.KakaoAccount("user@example.com", true, "male", "1995")));
		when(jwtTokenProvider.generateAccessToken(any(), any(), any())).thenReturn("access-jwt");
		when(jwtTokenProvider.generateRefreshToken(any())).thenReturn("refresh-jwt");
	}

	@Test
	@DisplayName("최초 로그인(소셜 계정 없음)이면 isNewUser=true")
	void firstLogin_returnsNewUser() {
		when(userSocialAccountRepository.findByProviderAndProviderUid(OauthProvider.KAKAO, KAKAO_ID.toString()))
				.thenReturn(Optional.empty());
		when(userRepository.existsByNickname(any())).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
		when(userSocialAccountRepository.save(any(UserSocialAccount.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		KakaoCallbackResponse response = authService.kakaoLogin(CODE, STATE);

		assertThat(response.isNewUser()).isTrue();
	}

	@Test
	@DisplayName("재로그인이지만 온보딩 미완료면 isNewUser=true (닉네임 미설정 후 이탈 케이스)")
	void reLogin_notOnboarded_returnsNewUser() {
		User notOnboarded = User.builder()
				.nickname("kakao_" + KAKAO_ID)
				.gender(GenderType.MALE)
				.age(30)
				.build();
		mockExistingSocialAccount(notOnboarded);

		KakaoCallbackResponse response = authService.kakaoLogin(CODE, STATE);

		assertThat(response.isNewUser()).isTrue();
	}

	@Test
	@DisplayName("재로그인이고 온보딩 완료면 isNewUser=false")
	void reLogin_onboarded_returnsExistingUser() {
		User onboarded = User.builder()
				.nickname("실제닉네임")
				.gender(GenderType.MALE)
				.age(30)
				.build();
		onboarded.completeOnboarding();
		mockExistingSocialAccount(onboarded);

		KakaoCallbackResponse response = authService.kakaoLogin(CODE, STATE);

		assertThat(response.isNewUser()).isFalse();
	}

	@Test
	@DisplayName("탈퇴 회원 재로그인 시 계정 복구 및 온보딩 상태 유지")
	void reLogin_deletedUser_reactivatesAndKeepsOnboardingStatus() {
		User deletedAndOnboarded = User.builder()
				.nickname("이전닉네임")
				.gender(GenderType.MALE)
				.age(30)
				.build();
		deletedAndOnboarded.completeOnboarding();
		deletedAndOnboarded.delete();  // 탈퇴 처리

		UserSocialAccount socialAccount = UserSocialAccount.builder()
				.user(deletedAndOnboarded)
				.provider(OauthProvider.KAKAO)
				.providerUid(KAKAO_ID.toString())
				.providerEmail("user@example.com")
				.build();
		when(userSocialAccountRepository.findByProviderAndProviderUid(OauthProvider.KAKAO, KAKAO_ID.toString()))
				.thenReturn(Optional.of(socialAccount));
		when(userSocialAccountRepository.save(any(UserSocialAccount.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		KakaoCallbackResponse response = authService.kakaoLogin(CODE, STATE);

		// 탈퇴된 사용자가 복구됨
		assertThat(deletedAndOnboarded.getDeletedAt()).isNull();
		assertThat(deletedAndOnboarded.getStatus().name()).isEqualTo("ACTIVE");
		// 온보딩 상태는 유지됨
		assertThat(response.isNewUser()).isFalse();
		// 닉네임은 유지됨
		assertThat(deletedAndOnboarded.getNickname()).isEqualTo("이전닉네임");
	}

	private void mockExistingSocialAccount(User user) {
		UserSocialAccount socialAccount = UserSocialAccount.builder()
				.user(user)
				.provider(OauthProvider.KAKAO)
				.providerUid(KAKAO_ID.toString())
				.providerEmail("user@example.com")
				.build();
		when(userSocialAccountRepository.findByProviderAndProviderUid(OauthProvider.KAKAO, KAKAO_ID.toString()))
				.thenReturn(Optional.of(socialAccount));
	}
}
