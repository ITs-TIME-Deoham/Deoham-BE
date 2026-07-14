package com.deoham.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deoham.auth.client.KakaoOAuthClient;
import com.deoham.auth.client.KakaoTokenResponse;
import com.deoham.auth.entity.OAuthState;
import com.deoham.auth.repository.OAuthStateRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.user.entity.OauthProvider;
import java.time.Instant;
import com.deoham.TestcontainersConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@DisplayName("AuthService Kakao OAuth validation test")
class AuthServiceKakaoFailureTest {

	private static final String REAL_CODE = "hYxbmmrEVi1e-YbCdXJz87kSK3FdtGvSaw7qJQeUwvhlWdlyLAu-qQAAAAQKFwvXAAABnzXlcvFONYg--5I0Sw";
	private static final String STATE_PREFIX = "bA9QpA_QLbX6CVfl01jCgFpRe9j9LwqGzcXXYeyto04";
	
	@Autowired
	private AuthService authService;

	@Autowired
	private OAuthStateRepository oauthStateRepository;

	@Autowired
	private KakaoOAuthClient kakaoOAuthClient;

	@Test
	@DisplayName("state only - 저장된 state이면 유효하다")
	void validateStateOnly_whenStateExistsAndNotExpired_thenValid() {
		String state = uniqueState("valid");
		oauthStateRepository.save(new OAuthState(
				state,
				OauthProvider.KAKAO,
				Instant.now().plusSeconds(300)));

		OAuthState savedState = oauthStateRepository
				.findByStateAndProvider(state, OauthProvider.KAKAO)
				.orElseThrow();

		assertThat(savedState.isExpired(Instant.now())).isFalse();
	}

	@Test
	@DisplayName("state only - 저장되지 않은 state이면 UNAUTHORIZED")
	void validateStateOnly_whenStateDoesNotExist_thenUnauthorized() {
		String missingState = uniqueState("missing");

		assertThatThrownBy(() -> authService.kakaoLogin("unused-code", missingState))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
					assertThat(ex.getMessage()).contains("Invalid OAuth state");
				});
	}

	@Test
	@DisplayName("state only - 만료된 state이면 UNAUTHORIZED")
	void validateStateOnly_whenStateExpired_thenUnauthorized() {
		String expiredState = uniqueState("expired");
		oauthStateRepository.save(new OAuthState(
				expiredState,
				OauthProvider.KAKAO,
				Instant.now().minusSeconds(1)));

		assertThatThrownBy(() -> authService.kakaoLogin("unused-code", expiredState))
				.isInstanceOfSatisfying(BusinessException.class, ex -> {
					assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
					assertThat(ex.getMessage()).contains("OAuth state has expired");
				});
	}

	@Test
	@Disabled("일회용 인가 코드를 소스에 고정해 두면 재실행이 불가능하다 — Kakao API는 WireMock 등으로 모킹해 검증할 것")
	@DisplayName("code only - Kakao token exchange로 code 유효성을 검증한다")
	void validateCodeOnly_whenKakaoAcceptsCode_thenTokenResponseIsReturned() {
		KakaoTokenResponse token = kakaoOAuthClient.exchangeCode(REAL_CODE);

		assertThat(token).isNotNull();
		assertThat(token.accessToken()).isNotBlank();
	}

	private String uniqueState(String label) {
		return STATE_PREFIX + "_" + label + "_" + System.nanoTime();
	}
}
