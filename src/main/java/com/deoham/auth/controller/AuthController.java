package com.deoham.auth.controller;

import com.deoham.auth.controller.docs.AuthControllerDocs;
import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.KakaoLoginResult;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.service.AuthService;
import com.deoham.global.config.HttpOnlyAuthProperties;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricEndpoint;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.CookieUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerDocs {

	private final AuthService authService;
	private final HttpOnlyAuthProperties httpOnlyAuthProperties;

	@Override
	@GetMapping("/kakao")
	@MetricEndpoint("auth.kakao.redirect")
	public ResponseEntity<Void> kakaoAuthRedirect() {
		return ResponseEntity.status(HttpStatus.FOUND)
				.location(authService.kakaoAuthorizationUri())
				.build();
	}

	@Override
	@PostMapping("/kakao/callback")
	@MetricEndpoint("auth.kakao.callback")
	public ResponseEntity<ApiResponse<KakaoCallbackResponse>> kakaoCallback(
			@RequestBody @Valid KakaoCallbackRequest request,
			HttpServletResponse response
	) {
		KakaoLoginResult loginResult = Objects.requireNonNull(
				authService.kakaoLogin(request.code(), request.state()),
				"Kakao login result must not be null"
		);

		// refreshToken을 HttpOnly 쿠키로 설정
		CookieUtils.setRefreshTokenCookie(
				response,
				loginResult.refreshToken(),
				httpOnlyAuthProperties.secureCookie()
		);

		// accessToken은 Authorization 헤더에 담아서 반환
		// Response body는 ApiResponse로 감싸서 반환 (CLAUDE.md 규칙 준수)
		return ResponseEntity.ok()
				.header("Authorization", "Bearer " + loginResult.accessToken())
				.body(ApiResponse.ok(new KakaoCallbackResponse(loginResult.isNewUser())));
	}

	@Override
	@PostMapping("/refresh")
	@MetricEndpoint("auth.refresh")
	public ResponseEntity<ApiResponse<Void>> refresh(
			HttpServletRequest request,
			HttpServletResponse response
	) {
		// 쿠키에서 refreshToken 자동 추출
		String refreshToken = CookieUtils.getRefreshTokenFromCookie(request);

		// refreshToken이 없으면 401 Unauthorized 반환
		if (refreshToken == null) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token is missing or invalid");
		}

		TokenResponse tokenResponse = authService.refresh(refreshToken);

		// accessToken은 Authorization 헤더에만 담기
		var responseBuilder = ResponseEntity.ok()
				.header("Authorization", "Bearer " + tokenResponse.accessToken());

		// 새로운 refreshToken이 있으면 쿠키에만 설정
		if (tokenResponse.refreshToken() != null) {
			CookieUtils.setRefreshTokenCookie(
					response,
					tokenResponse.refreshToken(),
					httpOnlyAuthProperties.secureCookie()
			);
		}

		return responseBuilder.body(ApiResponse.ok(null));
	}

	@Override
	@PostMapping("/logout")
	@MetricEndpoint("auth.logout")
	public ResponseEntity<ApiResponse<Void>> logout(
			Authentication authentication,
			HttpServletResponse response
	) {
		authService.logout(authentication);

		// refreshToken 쿠키 삭제
		CookieUtils.deleteRefreshTokenCookie(response, httpOnlyAuthProperties.secureCookie());

		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
