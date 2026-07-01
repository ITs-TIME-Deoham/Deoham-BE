package com.deoham.global.security;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class AuthenticationUtils {

	private AuthenticationUtils() {
	}

	public static Optional<AuthPrincipal> currentPrincipal() {
		return fromAuthentication(SecurityContextHolder.getContext().getAuthentication());
	}

	public static AuthPrincipal requireCurrentPrincipal() {
		return currentPrincipal()
				.orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
	}

	public static UUID requireCurrentUserId() {
		return requireCurrentPrincipal().userId();
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
				jwt.getClaimAsString("role"),
				null,
				null,
				null
		));
	}
}
