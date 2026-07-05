package com.deoham.card.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "내 활성 카드 조회 응답 (온보딩 정보 포함)")
public record MyActiveCardResponse(
		@Schema(description = "활성 카드 상세정보 (없으면 null)")
		CardDetailResponse card,

		@Schema(description = "사용자가 이전에 카드를 생성한 이력이 있는지 여부", example = "false")
		boolean hasCreatedCard
) {
	public static MyActiveCardResponse of(CardDetailResponse card, boolean hasCreatedCard) {
		return MyActiveCardResponse.builder()
				.card(card)
				.hasCreatedCard(hasCreatedCard)
				.build();
	}
}
