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
	// 정상 응답이 ~1.5s 수준이므로 실패 판정까지 10s는 과함. 여유를 두고 4s로 단축(꼬리 지연 축소).
	// 지속 실패 시에는 FailoverTranslationProvider 의 서킷 브레이커가 DeepL 시도 자체를 건너뛴다.
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(4);

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