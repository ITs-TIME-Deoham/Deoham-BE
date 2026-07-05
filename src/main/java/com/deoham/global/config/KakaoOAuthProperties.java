package com.deoham.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "deoham.kakao")
public record KakaoOAuthProperties(
		String restApiKey,
		String clientSecret,
		String redirectUri
) {
}
