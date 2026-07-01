package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card summary response")
public record CardSummaryResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "Category")
        CardCategory category,

        @Schema(description = "Status")
        CardStatus status,

        @Schema(description = "Expiration time")
        Instant expiresAt,

        @Schema(description = "Preferred gender")
        PreferredGender preferredGender,

        @Schema(description = "Preferred minimum age")
        Integer preferredAgeMin,

        @Schema(description = "Preferred maximum age")
        Integer preferredAgeMax,

        @Schema(description = "Distance from current location in meters")
        Double distanceMeters,

        @Schema(description = "Created time")
        Instant createdAt
) {
}
