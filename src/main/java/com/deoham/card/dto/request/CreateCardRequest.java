package com.deoham.card.dto.request;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Card 생성 요청")
public record CreateCardRequest(

        @NotNull
        @Schema(description = "카테고리", example = "PHOTO/MEAL/RIDE/OTHER")
        CardCategory category,

        @Schema(description = "제목 (최대 100자)", example = "사진 찍어주실 분 구해요", maxLength = 100)
        String title,

        @Schema(description = "상세 내용", example = "한강에서 사진 찍어주실 분 구합니다!")
        String description,

        @NotNull
        @Schema(description = "위도", example = "37.5326")
        Double latitude,

        @NotNull
        @Schema(description = "경도", example = "126.9903")
        Double longitude,

        @Schema(description = "선호 성별 (미입력 시 ANY)", example = "MALE/FEMALE/ANY")
        PreferredGender preferredGender,

        @Schema(description = "선호 최소 나이", example = "20")
        Integer preferredAgeMin,

        @Schema(description = "선호 최대 나이", example = "35")
        Integer preferredAgeMax
) {
}
