package com.deoham.card.dto.request;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Schema(description = "Card create request")
public record CreateCardRequest(

        @NotNull
        @Schema(description = "Category", example = "PHOTO")
        CardCategory category,

        @NotBlank
        @Size(max = 100)
        @Schema(description = "Title", example = "Help me take a photo", maxLength = 100)
        String title,

        @Schema(description = "Description", example = "경복궁 어디어디에서 몇시에 사진 찍어주실 분 있나요?")
        String description,

        @NotNull
        @Schema(description = "Latitude", example = "37.5326")
        Double latitude,

        @NotNull
        @Schema(description = "Longitude", example = "126.9903")
        Double longitude,

        @Size(max = 100)
        @Schema(description = "City", example = "Seoul", maxLength = 100)
        String city,

        @Schema(description = "Search radius in meters", example = "1000")
        Integer radiusM,

        @NotNull
        @Schema(description = "Expiration time", example = "2026-06-30T12:00:00Z")
        Instant expiresAt,

        @Schema(description = "Preferred gender", example = "MALE, FEMALE, ANY")
        PreferredGender preferredGender,

        @Schema(description = "Preferred minimum age", example = "20")
        Integer preferredAgeMin,

        @Schema(description = "Preferred maximum age", example = "50")
        Integer preferredAgeMax
) {
}
