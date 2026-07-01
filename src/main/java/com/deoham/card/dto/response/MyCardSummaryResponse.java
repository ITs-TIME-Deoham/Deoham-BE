package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "My card summary response")
public record MyCardSummaryResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "Category")
        CardCategory category,

        @Schema(description = "Status")
        CardStatus status,

        @Schema(description = "Expiration time")
        Instant expiresAt,

        @Schema(description = "Created time")
        Instant createdAt
) {
}
