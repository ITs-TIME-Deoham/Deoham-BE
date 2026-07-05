package com.deoham.auth.dto;

import com.deoham.user.entity.User;

public record ProfileUpdateResponse(
		String nickname,
		String profileImageUrl
) {
	public static ProfileUpdateResponse from(User user) {
		return new ProfileUpdateResponse(user.getNickname(), user.getProfileImageUrl());
	}
}
