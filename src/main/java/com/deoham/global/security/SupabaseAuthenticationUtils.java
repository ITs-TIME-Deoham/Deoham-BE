package com.deoham.global.security;

import java.util.Map;
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
		String email = jwt.getClaimAsString("email");

		Map<String, Object> appMeta = jwt.getClaimAsMap("app_metadata");
		String provider = (appMeta != null) ? (String) appMeta.get("provider") : null;

		Map<String, Object> userMeta = jwt.getClaimAsMap("user_metadata");
		String name = null;
		if (userMeta != null) {
			name = (String) userMeta.get("full_name");
			if (name == null) {
				name = (String) userMeta.get("name");
			}
		}
		if (name == null || name.isBlank()) {
			name = (email != null && email.contains("@")) ? email.split("@")[0] : "사용자";
		}

		return Optional.of(new SupabasePrincipal(
				userId,
				email,
				jwt.getClaimAsString("role"),
				provider,
				name
		));
	}
}
