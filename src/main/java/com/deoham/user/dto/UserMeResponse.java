package com.deoham.user.dto;

import com.deoham.user.entity.JobType;
import com.deoham.user.entity.PlanType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "내 프로필 응답")
public record UserMeResponse(
        @Schema(description = "유저 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "이메일", example = "user@example.com")
        String email,

        @Schema(description = "이름", example = "홍길동")
        String name,

        @Schema(description = "직군")
        JobType jobType,

        @Schema(description = "전화번호", example = "010-1234-5678")
        String phone,

        @Schema(description = "요금제")
        PlanType planType,

        @Schema(description = "새 카드 알림 수신 여부")
        boolean notiNewCard,

        @Schema(description = "링크 조회 알림 수신 여부")
        boolean notiLinkViewed,

        @Schema(description = "상대방 확인 알림 수신 여부")
        boolean notiCounterpartConfirmed
) {
}
