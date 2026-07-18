package com.deoham.auth.service;

import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.client.KakaoUserInfo;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.global.config.KakaoOAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.security.AuthenticationUtils;
import com.deoham.global.security.JwtProperties;
import com.deoham.global.security.JwtTokenProvider;
import com.deoham.user.entity.GenderType;
import com.deoham.user.entity.OauthProvider;
import com.deoham.user.entity.User;
import com.deoham.user.entity.UserSocialAccount;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.repository.UserSocialAccountRepository;
import java.net.URI;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AuthService {

	private static final long OAUTH_STATE_EXPIRY_SECONDS = 300;
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();

	private final KakaoOAuthClient kakaoOAuthClient;
	private final KakaoOAuthProperties kakaoOAuthProperties;
	private final JwtTokenProvider jwtTokenProvider;
	private final JwtProperties jwtProperties;
	private final UserRepository userRepository;
	private final UserSocialAccountRepository userSocialAccountRepository;
	private final OAuthStateRepository oauthStateRepository;

	@Transactional
	public URI kakaoAuthorizationUri() {
		oauthStateRepository.deleteByExpiresAtBefore(Instant.now());

		String state = generateState();
		oauthStateRepository.save(new OAuthState(
				state,
				OauthProvider.KAKAO,
				Instant.now().plusSeconds(OAUTH_STATE_EXPIRY_SECONDS)));

		return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
				.queryParam("client_id", kakaoOAuthProperties.restApiKey())
				.queryParam("redirect_uri", kakaoOAuthProperties.redirectUri())
				.queryParam("response_type", "code")
				.queryParam("scope", "account_email,gender,birthyear")
				.queryParam("state", state)
				.build()
				.toUri();
	}

	@Transactional
	public KakaoCallbackResponse kakaoLogin(String code, String state) {
		consumeOAuthState(state, OauthProvider.KAKAO);

		KakaoTokenResponse kakaoToken = kakaoOAuthClient.exchangeCode(code);
		KakaoUserInfo userInfo = kakaoOAuthClient.getUserInfo(kakaoToken.accessToken());
		String kakaoId = userInfo.id().toString();

		UserSocialAccount socialAccount = userSocialAccountRepository
				.findByProviderAndProviderUid(OauthProvider.KAKAO.name(), kakaoId)
				.orElse(null);

		User user;
		if (socialAccount == null) {
			user = userRepository.save(User.builder()
					.nickname(generateDefaultNickname(kakaoId))
					.gender(parseGender(userInfo.gender()))
					.age(calculateAge(userInfo.birthyear()))
					.build());

			socialAccount = userSocialAccountRepository.save(UserSocialAccount.builder()
					.user(user)
					.provider(OauthProvider.KAKAO)
					.providerUid(kakaoId)
					.providerEmail(userInfo.email())
					.build());
		} else {
			user = userRepository.findByIdIncludeDeleted(socialAccount.getUserId())
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "User not found"));
			if (user.getDeletedAt() != null) {
				user.reactivate();
			}
			user.updateAge(calculateAge(userInfo.birthyear()));
		}

		String accessToken = jwtTokenProvider.generateAccessToken(
				user.getId(), userInfo.email(), user.getRole().name());
		String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
		socialAccount.updateTokens(
				null,
				refreshToken,
				Instant.now().plusSeconds(jwtProperties.refreshTokenExpirySeconds()));
		userSocialAccountRepository.save(socialAccount);

		// "신규 사용자"는 레코드가 방금 생성됐는지가 아니라 온보딩(닉네임 설정)을 마쳤는지로 판단한다.
		// 콜백에서 User/UserSocialAccount는 즉시 저장되므로, 닉네임 설정 없이 이탈 후 재로그인해도
		// 온보딩이 미완료면 계속 신규 사용자로 안내한다.
		boolean needsOnboarding = !user.isOnboardingCompleted();

		return new KakaoCallbackResponse(
				accessToken,
				refreshToken,
				"Bearer",
				jwtProperties.accessTokenExpirySeconds(),
				needsOnboarding);
	}

	@Transactional
	public TokenResponse refresh(String refreshToken) {
		Jwt jwt;
		try {
			jwt = jwtTokenProvider.parseToken(refreshToken);
		} catch (JwtException e) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid refresh token.");
		}
		if (jwt.getExpiresAt() != null && jwt.getExpiresAt().isBefore(Instant.now())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token has expired.");
		}
		if (!"refresh".equals(jwt.getClaimAsString("type"))) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Token is not a refresh token.");
		}

		UUID userId = UUID.fromString(jwt.getSubject());
		UserSocialAccount socialAccount = userSocialAccountRepository.findByRefreshToken(refreshToken)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is not registered."));
		if (!socialAccount.getUser().getId().equals(userId)) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token user does not match.");
		}
		if (socialAccount.getTokenExpiresAt() == null || !socialAccount.getTokenExpiresAt().isAfter(Instant.now())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token has expired.");
		}

		User user = socialAccount.getUser();
		String newAccessToken = jwtTokenProvider.generateAccessToken(
				user.getId(), socialAccount.getProviderEmail(), user.getRole().name());
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
		socialAccount.updateTokens(
				null,
				newRefreshToken,
				Instant.now().plusSeconds(jwtProperties.refreshTokenExpirySeconds()));

		return new TokenResponse(
				newAccessToken,
				newRefreshToken,
				"Bearer",
				jwtProperties.accessTokenExpirySeconds());
	}

	@Transactional
	public void logout(Authentication authentication) {
		UUID userId = AuthenticationUtils.requiredUserId(authentication);
		userSocialAccountRepository.findAllByUser_Id(userId)
				.forEach(UserSocialAccount::revokeRefreshToken);
	}

	private void consumeOAuthState(String state, OauthProvider provider) {
		OAuthState oauthState = oauthStateRepository.findByStateAndProvider(state, provider)
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid OAuth state."));
		oauthStateRepository.delete(oauthState);
		if (oauthState.isExpired(Instant.now())) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "OAuth state has expired.");
		}
	}

	private static String generateState() {
		byte[] bytes = new byte[32];
		SECURE_RANDOM.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String generateDefaultNickname(String kakaoId) {
		String base = "kakao_" + kakaoId;
		if (!userRepository.existsByNickname(base)) {
			return base;
		} 
		for (int suffix = 1; suffix < 1000; suffix++) {
			String candidate = base + "_" + suffix;
			if (!userRepository.existsByNickname(candidate)) {
				return candidate;
			}
		}
		throw new BusinessException(ErrorCode.CONFLICT, "Could not generate a default nickname.");
	}

	private GenderType parseGender(String gender) {
		if (gender == null || gender.isBlank()) {
			throw new BusinessException(ErrorCode.INVALID_REQUEST, "성별 정보가 필요합니다");
		}
		return switch (gender.toUpperCase()) {
			case "MALE" -> GenderType.MALE;
			case "FEMALE" -> GenderType.FEMALE;
			default -> throw new BusinessException(ErrorCode.INVALID_REQUEST, "유효하지 않은 성별 정보입니다");
		};
	}

	private Integer calculateAge(String birthyear) {
		if (birthyear == null || birthyear.isBlank()) {
			return null;
		}
		try {
			int year = Integer.parseInt(birthyear);
			int currentYear = LocalDate.now().getYear();
			return currentYear - year;
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
