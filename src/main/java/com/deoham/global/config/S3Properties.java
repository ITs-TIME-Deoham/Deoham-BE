package com.deoham.global.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "deoham.s3")
public record S3Properties(
		@NotBlank String region,
		@NotBlank String bucket,
		@Positive long presignedUrlTtlSeconds
) {
}
