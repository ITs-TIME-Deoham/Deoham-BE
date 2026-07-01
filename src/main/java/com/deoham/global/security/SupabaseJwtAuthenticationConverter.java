package com.deoham.global.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class SupabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		Collection<GrantedAuthority> authorities = new ArrayList<>();

		String role = jwt.getClaimAsString("role");
		if (role != null && !role.isBlank()) {
			authorities.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
		}

		List<String> scopes = jwt.getClaimAsStringList("scope");
		if (scopes != null) {
			scopes.stream()
					.filter(Objects::nonNull)
					.map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
					.forEach(authorities::add);
		}

		return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
	}
}
