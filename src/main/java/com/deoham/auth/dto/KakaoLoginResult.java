package com.deoham.auth.dto;

/**
 * AuthService.kakaoLogin()의 내부 반환 DTO
 * - accessToken: Authorization 헤더에 설정
 * - refreshToken: HttpOnly 쿠키에 설정
 * - isNewUser: 응답 바디에 포함
 */
public record KakaoLoginResult(
		String accessToken,
		String refreshToken,
		boolean isNewUser
) {
}
