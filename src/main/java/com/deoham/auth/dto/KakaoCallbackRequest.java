package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Kakao OAuth 콜백 요청")
public record KakaoCallbackRequest(

		@NotBlank(message = "인증 코드는 필수입니다")
		@Schema(description = "카카오 OAuth 인증 코드", example = "abc123...")
		String code,

		@Schema(description = "OAuth state (CSRF 방지)", example = "xyz789...")
		String state
) {
}
