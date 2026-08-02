package com.deoham.chat.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * {@link FailoverTranslationProvider}는 {@code @Profile("!test")}라 어떤 컨텍스트 테스트에서도 생성되지 않는다.
 * 그래서 생성자를 하나 더 추가해 Spring이 주입 대상을 고르지 못하게 되어도 테스트는 전부 통과하고,
 * 운영 기동 시점에야 {@code NoSuchMethodException: <init>()}으로 터진다(실제 배포 장애 발생).
 *
 * <p>이 테스트는 그 구멍을 막는다 — 실제 Spring 생성자 선택 로직에 클래스를 그대로 태워본다.
 */
class FailoverTranslationProviderWiringTest {

    @Test
    @DisplayName("Spring이 주입 생성자를 스스로 선택해 빈을 생성할 수 있다")
    void springCanInstantiateBean() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        AutowiredAnnotationBeanPostProcessor processor = new AutowiredAnnotationBeanPostProcessor();
        processor.setBeanFactory(beanFactory);
        beanFactory.addBeanPostProcessor(processor);

        beanFactory.registerSingleton("deepLTranslationProvider", mock(DeepLTranslationProvider.class));
        beanFactory.registerSingleton("geminiTranslationProvider", mock(GeminiTranslationProvider.class));
        beanFactory.registerBeanDefinition(
                "failoverTranslationProvider", new RootBeanDefinition(FailoverTranslationProvider.class));

        assertThatCode(() -> beanFactory.getBean("failoverTranslationProvider")).doesNotThrowAnyException();
        assertThat(beanFactory.getBean("failoverTranslationProvider"))
                .isInstanceOf(FailoverTranslationProvider.class);
    }
}
