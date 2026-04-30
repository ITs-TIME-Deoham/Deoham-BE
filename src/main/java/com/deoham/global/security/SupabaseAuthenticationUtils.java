package com.deoham.global.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SupabaseAuthenticationUtils {

	private SupabaseAuthenticationUtils() {
	}

	public static Optional<SupabasePrincipal> currentPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
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
		return Optional.of(new SupabasePrincipal(
				userId,
				jwt.getClaimAsString("email"),
				jwt.getClaimAsString("role")
		));
	}
}
