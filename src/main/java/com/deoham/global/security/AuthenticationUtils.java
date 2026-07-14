package com.deoham.global.security;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthenticationUtils {

	private AuthenticationUtils() {
	}

	/** 현재 SecurityContext의 인증 주체. 없으면 401 BusinessException. */
	public static AuthPrincipal requiredPrincipal() {
		return currentPrincipal().orElseThrow(AuthenticationUtils::unauthorized);
	}

	/** 현재 SecurityContext의 사용자 UUID. 없으면 401 BusinessException. */
	public static UUID requiredUserId() {
		return requiredPrincipal().userId();
	}

	/** 컨트롤러 파라미터로 주입된 Authentication에서 사용자 UUID 추출. 없으면 401 BusinessException. */
	public static UUID requiredUserId(Authentication authentication) {
		return fromAuthentication(authentication)
				.orElseThrow(AuthenticationUtils::unauthorized)
				.userId();
	}

	/** STOMP 핸들러의 Principal에서 사용자 UUID 추출. 없으면 401 BusinessException. */
	public static UUID requiredUserId(Principal principal) {
		if (!(principal instanceof Authentication authentication)) {
			throw unauthorized();
		}
		return requiredUserId(authentication);
	}

	private static BusinessException unauthorized() {
		return new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다.");
	}

	public static Optional<AuthPrincipal> currentPrincipal() {
		return fromAuthentication(SecurityContextHolder.getContext().getAuthentication());
	}

	public static Optional<AuthPrincipal> fromAuthentication(Authentication authentication) {
		if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
			return Optional.empty();
		}
		Jwt jwt = jwtAuth.getToken();
		String sub = jwt.getSubject();
		if (sub == null) {
			return Optional.empty();
		}
		UUID userId;
		try {
			userId = UUID.fromString(sub);
		} catch (IllegalArgumentException ex) {
			return Optional.empty();
		}
		return Optional.of(new AuthPrincipal(
				userId,
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("role")
		));
	}
}
