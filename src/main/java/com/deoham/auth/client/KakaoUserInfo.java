package com.deoham.auth.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoUserInfo(
		Long id,
		@JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {

	public record KakaoAccount(
			String email,
			@JsonProperty("email_verified") Boolean emailVerified,
			String gender,
			@JsonProperty("birthyear") String birthyear
	) {
	}

	public String email() {
		return kakaoAccount != null ? kakaoAccount.email() : null;
	}

	public String gender() {
		return kakaoAccount != null ? kakaoAccount.gender() : null;
	}

	public String birthyear() {
		return kakaoAccount != null ? kakaoAccount.birthyear() : null;
	}
}
