package com.deoham.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HttpOnly 기반 인증 설정
 * - local 환경: Secure 플래그 비활성화 (localhost 지원)
 * - prod 환경: Secure 플래그 활성화 (HTTPS 필수)
 */
@ConfigurationProperties(prefix = "app.auth.httponly")
public record HttpOnlyAuthProperties(
		/**
		 * 쿠키의 Secure 플래그 활성화 여부
		 * - true: HTTPS 연결만 쿠키 전송 (프로덕션)
		 * - false: HTTP도 허용 (로컬 개발)
		 */
		boolean secureCookie
) {
	public static final HttpOnlyAuthProperties DEFAULT = new HttpOnlyAuthProperties(true);
}
