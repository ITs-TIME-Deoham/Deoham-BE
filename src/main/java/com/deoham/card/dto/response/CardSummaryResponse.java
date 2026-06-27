package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card 요약 응답")
public record CardSummaryResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "카테고리")
        CardCategory category,

        @Schema(description = "제목")
        String title,

        @Schema(description = "상태")
        CardStatus status,

        @Schema(description = "선호 성별")
        PreferredGender preferredGender,

        @Schema(description = "선호 최소 나이")
        Integer preferredAgeMin,

        @Schema(description = "선호 최대 나이")
        Integer preferredAgeMax,

        @Schema(description = "현재 위치로부터의 거리 (미터)")
        Double distanceMeters,

        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
