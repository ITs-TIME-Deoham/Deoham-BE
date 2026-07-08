package com.deoham.card.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Paginated card list response")
public record PaginatedCardListResponse(

        @Schema(description = "Card list (max 20 items)")
        List<CardDetailResponse> cards,

        @Schema(description = "Cursor for the next page. Pass this value as the cursor query parameter in the next GET /api/cards/nearby request. It is Base64 encoded from the last card's distance and card ID. Null means there are no more cards.")
        String nextCursor,

        @Schema(description = "Whether the user has seen the card view onboarding. True if this is the first time viewing nearby cards.")
        Boolean hasSeenCardViewOnboarding
) {
}
