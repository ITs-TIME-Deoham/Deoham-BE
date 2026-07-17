package com.deoham.auth.controller;

import com.deoham.auth.controller.docs.AuthControllerDocs;
import com.deoham.auth.dto.KakaoCallbackRequest;
import com.deoham.auth.dto.KakaoCallbackResponse;
import com.deoham.auth.dto.RefreshTokenRequest;
import com.deoham.auth.dto.TokenResponse;
import com.deoham.auth.service.AuthService;
import com.deoham.global.metrics.MetricEndpoint;
import com.deoham.global.response.ApiResponse;
import jakarta.validation.Valid;
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
	public ResponseEntity<KakaoCallbackResponse> kakaoCallback(
			@RequestBody @Valid KakaoCallbackRequest request
	) {
		return ResponseEntity.ok(authService.kakaoLogin(request.code(), request.state()));
	}

	@Override
	@PostMapping("/refresh")
	@MetricEndpoint("auth.refresh")
	public ResponseEntity<ApiResponse<TokenResponse>> refresh(
			@RequestBody @Valid RefreshTokenRequest request
	) {
		return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request.refreshToken())));
	}

	@Override
	@PostMapping("/logout")
	@MetricEndpoint("auth.logout")
	public ResponseEntity<ApiResponse<Void>> logout(Authentication authentication) {
		authService.logout(authentication);
		return ResponseEntity.ok(ApiResponse.ok(null));
	}
}
