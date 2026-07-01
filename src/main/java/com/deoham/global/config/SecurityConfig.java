package com.deoham.global.config;

import com.deoham.global.security.RestAccessDeniedHandler;
import com.deoham.global.security.RestAuthenticationEntryPoint;
import com.deoham.global.security.SupabaseJwtAuthenticationConverter;
import com.deoham.global.security.SupabaseJwtProperties;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({ SupabaseJwtProperties.class, CorsProperties.class, SupabaseAuthProperties.class })
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_ENDPOINTS = {
			"/actuator/health",
			"/actuator/health/**",
			"/actuator/info",
			"/swagger-ui.html",
			"/swagger-ui/**",
			"/v3/api-docs",
			"/v3/api-docs/**",
			"/api/auth/signup",
			"/api/auth/login",
			"/api/auth/kakao"
	};

	private final SupabaseJwtProperties jwtProperties;
	private final CorsProperties corsProperties;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;

	@Bean
	public SupabaseJwtAuthenticationConverter jwtAuthenticationConverter() {
		return new SupabaseJwtAuthenticationConverter();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder,
			SupabaseJwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
						.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt
								.decoder(jwtDecoder)
								.jwtAuthenticationConverter(jwtAuthenticationConverter))
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler))
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwtProperties.jwksUri()).build();

		OAuth2TokenValidator<Jwt> defaultValidator = JwtValidators.createDefault();
		OAuth2TokenValidator<Jwt> issuerValidator = new JwtIssuerValidator(jwtProperties.issuer());
		OAuth2TokenValidator<Jwt> audienceValidator = audienceValidator(jwtProperties.audience());

		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				defaultValidator, issuerValidator, audienceValidator));
		return decoder;
	}

	private static OAuth2TokenValidator<Jwt> audienceValidator(String requiredAudience) {
		OAuth2Error error = new OAuth2Error(
				"invalid_token",
				"Required audience '" + requiredAudience + "' not present",
				null);
		return jwt -> {
			Object aud = jwt.getClaim("aud");
			boolean valid;
			if (aud instanceof String s) {
				valid = s.equals(requiredAudience);
			} else if (aud instanceof Collection<?> c) {
				valid = c.contains(requiredAudience);
			} else {
				valid = false;
			}
			return valid ? OAuth2TokenValidatorResult.success() : OAuth2TokenValidatorResult.failure(error);
		};
	}

	private CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(corsProperties.allowedOrigins());
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of("*"));
		config.setExposedHeaders(List.of("Location", "Content-Disposition"));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
