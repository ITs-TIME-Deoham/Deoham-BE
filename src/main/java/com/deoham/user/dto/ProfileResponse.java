package com.deoham.user.dto;

import com.deoham.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "프로필 정보 조회 응답")
public record ProfileResponse(
		@Schema(description = "사용자 닉네임", example = "홍길동")
		String nickname,

		@Schema(description = "프로필 이미지 URL", example = "https://example.com/image.jpg")
		String profileImageUrl,

		@Schema(description = "도움을 받은 횟수", example = "5")
		int helpRequestCount,

		@Schema(description = "도움을 준 횟수", example = "3")
		int helpCount,

		@Schema(description = "첫 카드 생성 여부 (온보딩 튜토리얼용)", example = "false")
		boolean hasCreatedCard
) {
	public static ProfileResponse from(User user) {
		return ProfileResponse.builder()
				.nickname(user.getNickname())
				.profileImageUrl(user.getProfileImageUrl())
				.helpRequestCount(user.getHelpRequestCount())
				.helpCount(user.getHelpCount())
				.hasCreatedCard(user.isHasCreatedCard())
				.build();
	}
}
