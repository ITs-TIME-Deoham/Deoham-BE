package com.deoham.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "내 프로필 응답")
public record UserMeResponse(
        @Schema(description = "유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "성별")
        String gender,

        @Schema(description = "나이")
        Integer age
) {
}
