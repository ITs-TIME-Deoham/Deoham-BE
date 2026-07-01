package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "토큰 갱신 요청")
public record RefreshTokenRequest(

        @Schema(description = "리프레시 토큰")
        @NotBlank
        String refreshToken
) {
}
