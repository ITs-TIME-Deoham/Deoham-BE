package com.deoham.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.kakao")
public record KakaoOAuthProperties(
        @NotBlank String restApiKey,
        @NotBlank String redirectUri
) {
}
