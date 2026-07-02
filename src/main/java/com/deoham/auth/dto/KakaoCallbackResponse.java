package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 로그인 콜백 응답")
public record KakaoCallbackResponse(
		@Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
		String accessToken,

		@Schema(description = "리프레시 토큰", example = "v1.abc123...")
		String refreshToken,

		@Schema(description = "토큰 타입", example = "Bearer")
		String tokenType,

		@Schema(description = "액세스 토큰 만료 시간 (초)", example = "3600")
		long expiresIn,

		@Schema(description = "신규 사용자 여부", example = "true")
		boolean isNewUser
) {
}
