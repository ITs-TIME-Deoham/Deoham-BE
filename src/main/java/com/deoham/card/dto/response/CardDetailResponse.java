package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card detail response")
public record CardDetailResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "Requester ID")
        UUID requesterId,

        @Schema(description = "Requester profile image URL")
        String requesterProfileImageUrl,

        @Schema(description = "Category")
        CardCategory category,

        @Schema(description = "Description")
        String description,

        @Schema(description = "Expiration time")
        Instant expiresAt,

        @Schema(description = "Status")
        CardStatus status,

        @Schema(description = "Preferred gender")
        PreferredGender preferredGender,

        @Schema(description = "Preferred minimum age")
        Integer preferredAgeMin,

        @Schema(description = "Preferred maximum age")
        Integer preferredAgeMax,

        @Schema(description = "Retry count")
        int retryCount,

        @Schema(description = "Created time")
        Instant createdAt,

        @Schema(description = "Updated time")
        Instant updatedAt
) {
}
