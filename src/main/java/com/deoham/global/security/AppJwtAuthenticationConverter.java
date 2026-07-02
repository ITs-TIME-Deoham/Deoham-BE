package com.deoham.global.security;

import java.util.Arrays;
import java.util.Collection;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AppJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		String role = jwt.getClaimAsString("role");
		Collection<GrantedAuthority> authorities = role != null
				? Arrays.asList(new SimpleGrantedAuthority("ROLE_" + role))
				: Arrays.asList();
		return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
	}
}
