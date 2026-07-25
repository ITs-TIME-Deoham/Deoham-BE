package com.deoham.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * RefreshToken 쿠키를 파싱하는 필터
 * - 쿠키에서 refreshToken 추출
 * - 요청 속성에 저장 (나중에 필요시 접근 가능)
 * - Authorization 헤더에는 accessToken만 포함되어야 함
 *
 * 주의: 이 필터는 정보 추출만 수행하고, 토큰 검증은 Spring Security가 담당
 */
@Slf4j
@Component
public class RefreshTokenFilter extends OncePerRequestFilter {

	public static final String REFRESH_TOKEN_ATTRIBUTE = "refreshToken";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		String refreshToken = CookieUtils.getRefreshTokenFromCookie(request);
		if (refreshToken != null) {
			request.setAttribute(REFRESH_TOKEN_ATTRIBUTE, refreshToken);
		}
		filterChain.doFilter(request, response);
	}
}
