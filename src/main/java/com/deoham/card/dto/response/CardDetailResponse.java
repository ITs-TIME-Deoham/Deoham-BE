package com.deoham.card.dto.response;

import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card 상세 응답")
public record CardDetailResponse(

        @Schema(description = "Card ID")
        UUID id,

        @Schema(description = "작성자 ID")
        UUID authorId,

        @Schema(description = "작성자 닉네임")
        String authorNickname,

        @Schema(description = "작성자 프로필 이미지 URL")
        String authorProfileImageUrl,

        @Schema(description = "카테고리")
        CardCategory category,

        @Schema(description = "제목")
        String title,

        @Schema(description = "상세 내용")
        String description,

        @Schema(description = "위도")
        Double latitude,

        @Schema(description = "경도")
        Double longitude,

        @Schema(description = "상태")
        CardStatus status,

        @Schema(description = "선호 성별")
        PreferredGender preferredGender,

        @Schema(description = "선호 최소 나이")
        Integer preferredAgeMin,

        @Schema(description = "선호 최대 나이")
        Integer preferredAgeMax,

        @Schema(description = "재시도 횟수")
        int retryCount,

        @Schema(description = "생성 시각")
        Instant createdAt,

        @Schema(description = "수정 시각")
        Instant updatedAt
) {
}
