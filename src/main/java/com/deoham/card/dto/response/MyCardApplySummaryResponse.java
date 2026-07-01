package com.deoham.card.dto.response;

import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "My card apply summary response")
public record MyCardApplySummaryResponse(

        @Schema(description = "Apply ID")
        UUID id,

        @Schema(description = "Card ID")
        UUID cardId,

        @Schema(description = "Card category")
        CardCategory cardCategory,

        @Schema(description = "Card status")
        CardStatus cardStatus,

        @Schema(description = "Apply status")
        CardApplyStatus status,

        @Schema(description = "Applied time")
        Instant appliedAt
) {
}
