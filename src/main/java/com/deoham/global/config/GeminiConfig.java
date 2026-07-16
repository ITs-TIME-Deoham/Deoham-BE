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

	/**
	 * Gemini 호출 전용 RestClient.Builder. 기본 자동 구성 빌더는 커넥트/리드 타임아웃이 없어
	 * API 지연 시 요청 스레드가 무한정 붙잡히므로 여기서만 타임아웃을 강제한다.
	 */
	@Bean
	@Qualifier("gemini")
	public RestClient.Builder geminiRestClientBuilder(RestClient.Builder restClientBuilder) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(CONNECT_TIMEOUT)
				.withReadTimeout(READ_TIMEOUT);
		return restClientBuilder.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings));
	}
}
