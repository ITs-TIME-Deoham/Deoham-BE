package com.deoham.auth.dto;

import jakarta.validation.constraints.Size;

public record ProfileUpdateRequest(
		@Size(min = 1, max = 50, message = "닉네임은 1자 이상 50자 이하여야 합니다")
		String nickname
) {
}
