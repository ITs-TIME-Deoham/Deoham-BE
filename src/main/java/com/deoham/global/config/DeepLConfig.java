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
@EnableConfigurationProperties(DeepLProperties.class)
public class DeepLConfig {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(10);

	/**
	 * DeepL 호출 전용 RestClient. GeminiConfig 와 동일한 이유로 커넥트/리드 타임아웃을
	 * 강제하고, 완성된 RestClient 를 노출해 기본 빌더 빈과의 타입 충돌을 피한다.
	 */
	@Bean
	@Qualifier("deepl")
	public RestClient deepLRestClient(RestClient.Builder restClientBuilder, DeepLProperties properties) {
		ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
				.withConnectTimeout(CONNECT_TIMEOUT)
				.withReadTimeout(READ_TIMEOUT);
		return restClientBuilder
				.baseUrl(properties.apiUrl())
				.requestFactory(ClientHttpRequestFactoryBuilder.detect().build(settings))
				.build();
	}
}