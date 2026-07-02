package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 리프레시 요청")
public record RefreshTokenRequest(

		@NotBlank(message = "리프레시 토큰은 필수입니다")
		@Schema(description = "리프레시 토큰", example = "v1.abc123...")
		String refreshToken
) {
}
