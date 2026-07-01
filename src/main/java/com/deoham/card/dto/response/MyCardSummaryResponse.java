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

        @Schema(description = "Title")
        String title,

        @Schema(description = "Status")
        CardStatus status,

        @Schema(description = "City")
        String city,

        @Schema(description = "Search radius in meters")
        Integer radiusM,

        @Schema(description = "Expiration time")
        Instant expiresAt,

        @Schema(description = "Created time")
        Instant createdAt
) {
}
