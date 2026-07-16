package com.deoham.user.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다")
		String nickname
) {
	public ProfileUpdateRequest {
		// nickname이 빈 문자열이면 null로 변환
		if (nickname != null && nickname.isBlank()) {
			nickname = null;
		}
	}
}
