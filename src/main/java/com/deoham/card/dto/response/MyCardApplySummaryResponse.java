package com.deoham.card.dto.response;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "내가 제출한 신청서 요약 응답")
public record MyCardApplySummaryResponse(

        @Schema(description = "신청서 ID")
        UUID id,

        @Schema(description = "Card ID")
        UUID cardId,

        @Schema(description = "Card 제목")
        String cardTitle,

        @Schema(description = "Card 카테고리")
        CardCategory cardCategory,

        @Schema(description = "Card 상태")
        CardStatus cardStatus,

        @Schema(description = "신청 상태")
        CardApplyStatus status,

        @Schema(description = "신청 시각")
        Instant createdAt
) {
}
