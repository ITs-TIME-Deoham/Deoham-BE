package com.deoham.global.config;

import com.deoham.global.security.RestAccessDeniedHandler;
import com.deoham.global.security.RestAuthenticationEntryPoint;
import com.deoham.global.security.jwt.JwtAuthenticationFilter;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({ CorsProperties.class, KakaoOAuthProperties.class })
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
			"/api/auth/kakao",          // Kakao OAuth 시작 (리다이렉트)
			"/api/auth/kakao/callback", // 인가 코드 → JWT 교환
			"/api/auth/refresh"         // 액세스 토큰 갱신
	};

	private final CorsProperties corsProperties;
	private final RestAuthenticationEntryPoint authenticationEntryPoint;
	private final RestAccessDeniedHandler accessDeniedHandler;
	private final JwtAuthenticationFilter jwtAuthenticationFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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
				.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
				.exceptionHandling(ex -> ex
						.authenticationEntryPoint(authenticationEntryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
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
