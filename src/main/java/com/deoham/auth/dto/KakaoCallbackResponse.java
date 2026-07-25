package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 카카오 로그인 콜백 응답
 * - accessToken: Authorization 헤더를 통해 전달
 * - refreshToken: HttpOnly 쿠키를 통해 전달
 * - isNewUser: 응답 바디에 포함
 */
@Schema(description = "카카오 로그인 콜백 응답")
public record KakaoCallbackResponse(
		@Schema(description = "신규 사용자 여부 (온보딩 미완료)", example = "true")
		boolean isNewUser
) {
}
