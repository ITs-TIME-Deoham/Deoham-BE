package com.deoham.global.security.jwt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive long accessTokenExpirySeconds,
        @Positive long refreshTokenExpirySeconds
) {}
