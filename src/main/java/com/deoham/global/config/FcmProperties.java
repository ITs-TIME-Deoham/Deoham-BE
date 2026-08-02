package com.deoham.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FCM 설정. 키 경로가 비어 있으면(로컬/CI) 발송을 비활성화하는 graceful 정책이므로
 * {@code serviceAccountKeyPath}에 {@code @NotBlank}를 걸지 않는다.
 */
@ConfigurationProperties(prefix = "fcm")
public record FcmProperties(
		String serviceAccountKeyPath
) {
}
