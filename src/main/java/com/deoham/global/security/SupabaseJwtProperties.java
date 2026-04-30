package com.deoham.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.supabase.jwt")
public record SupabaseJwtProperties(
		@NotBlank String issuer,
		@NotBlank String jwksUri,
		@NotBlank String audience
) {
}
