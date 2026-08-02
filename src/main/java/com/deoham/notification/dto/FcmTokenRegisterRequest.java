package com.deoham.notification.dto;

import com.deoham.notification.entity.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "FCM 토큰 등록 요청")
public record FcmTokenRegisterRequest(

        @Schema(
                description = "FCM 등록 토큰",
                example = "fGzY...:APA91b..."
        )
        @NotBlank(message = "FCM 토큰은 필수입니다")
        String token,

        @Schema(
                description = "기기 식별자 (앱이 생성/보관하는 값). 토큰 갱신 시 같은 기기 판별에 사용",
                example = "550e8400-e29b-41d4-a716-446655440000"
        )
        @NotBlank(message = "기기 식별자는 필수입니다")
        String deviceId,

        @Schema(
                description = "기기 플랫폼",
                example = "ANDROID",
                allowableValues = {"ANDROID", "IOS", "WEB"}
        )
        @NotNull(message = "플랫폼은 필수입니다")
        Platform platform
) {
}
