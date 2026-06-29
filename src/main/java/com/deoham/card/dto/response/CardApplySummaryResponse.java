package com.deoham.card.dto.response;

import com.deoham.card.entity.CardApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card 신청서 요약 응답")
public record CardApplySummaryResponse(

        @Schema(description = "신청서 ID")
        UUID id,

        @Schema(description = "신청자 ID")
        UUID applicantId,

        @Schema(description = "신청자 닉네임")
        String applicantNickname,

        @Schema(description = "신청자 프로필 이미지 URL")
        String applicantProfileImageUrl,

        @Schema(description = "신청 상태")
        CardApplyStatus status,

        @Schema(description = "신청 시각")
        Instant createdAt
) {
}
