package com.deoham.global.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseCookie;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CookieUtils {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
	private static final String REFRESH_TOKEN_PATH = "/api/auth";
	private static final int REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60; // 7 days

	/**
	 * Set refresh token as HttpOnly cookie
	 */
	public static void setRefreshTokenCookie(HttpServletResponse response, String refreshToken,
			boolean isSecure) {
		ResponseCookie cookie = ResponseCookie
				.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
				.path(REFRESH_TOKEN_PATH)
				.maxAge(REFRESH_TOKEN_MAX_AGE)
				.httpOnly(true)
				.secure(isSecure)
				.sameSite("Strict")
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	/**
	 * Delete refresh token cookie (by setting max-age to 0)
	 */
	public static void deleteRefreshTokenCookie(HttpServletResponse response, boolean isSecure) {
		ResponseCookie cookie = ResponseCookie
				.from(REFRESH_TOKEN_COOKIE_NAME, "")
				.path(REFRESH_TOKEN_PATH)
				.maxAge(0)
				.httpOnly(true)
				.secure(isSecure)
				.sameSite("Strict")
				.build();
		response.addHeader("Set-Cookie", cookie.toString());
	}

	/**
	 * Extract refresh token from request cookies
	 */
	public static String getRefreshTokenFromCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
				.filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}
}
