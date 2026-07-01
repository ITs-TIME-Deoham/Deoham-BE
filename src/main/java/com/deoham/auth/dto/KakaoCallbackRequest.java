package com.deoham.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "카카오 OAuth 콜백 요청")
public record KakaoCallbackRequest(

        @Schema(description = "Kakao 인가 코드 (Kakao OAuth에서 발급)", example = "abc123xyz")
        @NotBlank
        String code,

        @Schema(description = "OAuth state. /api/auth/kakao에서 발급된 값을 그대로 전달합니다.", example = "n3fRAwRjQquAAAE")
        @NotBlank
        String state
) {
}
