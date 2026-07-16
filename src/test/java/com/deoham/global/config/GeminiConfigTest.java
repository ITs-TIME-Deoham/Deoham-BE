package com.deoham.global.config;

import com.deoham.chat.translation.GeminiTranslationProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RestClientAutoConfiguration이 실제로 구성에 참여하는 환경에서 GeminiConfig/GeminiTranslationProvider가
 * 정상적으로 뜨는지 검증한다. geminiRestClient 빈이 RestClient.Builder 타입으로 노출되면
 * RestClientAutoConfiguration의 기본 restClientBuilder 빈이 @ConditionalOnMissingBean에 걸려
 * 등록되지 않고, 그 결과 이 빈 자체가 깨지는 회귀(CI에서 전체 SpringBootTest 컨텍스트 로딩 실패로 나타남)를 막는다.
 */
class GeminiConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(GeminiConfig.class, GeminiTranslationProvider.class)
            .withPropertyValues(
                    "deoham.gemini.api-key=test-api-key",
                    "deoham.gemini.model=gemini-3.5-flash")
            // gradle test 태스크가 JVM 전역에 spring.profiles.active=test 를 걸어두므로(build.gradle),
            // @Profile("!test") 컴포넌트가 실제 운영 프로파일에서 뜨는지 보려면 여기서 덮어써야 한다.
            .withSystemProperties("spring.profiles.active=");

    @Test
    void contextLoads_withGeminiRestClientAndTranslationProvider() {
        contextRunner.run((AssertableApplicationContext context) -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(GeminiTranslationProvider.class);
            assertThat(context.getBean("geminiRestClient")).isInstanceOf(RestClient.class);
        });
    }
}
