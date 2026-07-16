package com.deoham.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GeminiProperties.class)
public class GeminiConfig {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	private static final String BASE_URL = "https://generativelanguage.googleapis.com";

	/**
	 * Gemini 호출 전용 RestClient. 기본 자동 구성 빌더는 커넥트/리드 타임아웃이 없어
	 * API 지연 시 요청 스레드가 무한정 붙잡히므로 여기서만 타임아웃을 강제한다.
	 *
	 * <p>반환 타입을 RestClient.Builder 로 두면 RestClientAutoConfiguration 의
	 * {@code @ConditionalOnMissingBean} 조건에 걸려 기본 빌더 빈 자체가 등록되지 않고,
	 * 그 빈을 파라미터로 받는 이 메서드도 함께 깨진다. 완성된 RestClient 를 노출해 타입 충돌을 피한다.
	 */
	@Bean
	@Qualifier("gemini")
	public RestClient geminiRestClient(RestClient.Builder restClientBuilder) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(CONNECT_TIMEOUT)
				.withReadTimeout(READ_TIMEOUT);
		return restClientBuilder
				.baseUrl(BASE_URL)
				.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
				.build();
	}
}
