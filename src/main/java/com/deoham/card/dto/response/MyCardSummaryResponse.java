package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "내가 작성한 Card 요약 응답")
public record MyCardSummaryResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "카테고리")
        CardCategory category,

        @Schema(description = "제목")
        String title,

        @Schema(description = "상태")
        CardStatus status,

        @Schema(description = "생성 시각")
        Instant createdAt
) {
}
