package com.deoham.global.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.deepl")
public record DeepLProperties(
		@NotBlank String apiKey,
		@NotBlank String apiUrl
) {
}