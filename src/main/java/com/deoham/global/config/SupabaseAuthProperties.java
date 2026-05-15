package com.deoham.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.supabase.auth")
public record SupabaseAuthProperties(
        @NotBlank String url,
        @NotBlank String anonKey,
        @NotBlank String redirectUrl
) {
}
