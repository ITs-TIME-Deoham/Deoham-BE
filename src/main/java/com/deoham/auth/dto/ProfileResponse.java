package com.deoham.auth.dto;

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
		int helpCount
) {
	public static ProfileResponse from(User user) {
		return ProfileResponse.builder()
				.nickname(user.getNickname())
				.profileImageUrl(user.getProfileImageUrl())
				.helpRequestCount(user.getHelpRequestCount())
				.helpCount(user.getHelpCount())
				.build();
	}
}
