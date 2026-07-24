package com.deoham.chat.translation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DeepL을 우선 시도하고, 실패(네트워크 오류·인증 오류·미지원 언어 등 어떤 이유든)하면
 * Gemini로 자동 전환한다. 어느 provider가 실제로 처리했는지는 {@link TranslationResult#providerName()}에
 * 담겨 호출자에게 그대로 전달되므로, 이 클래스 자체는 상태를 갖지 않는다(동시 요청 간 경합 없음).
 */
@Slf4j
@Primary
@Profile("!test")
@Component
public class FailoverTranslationProvider implements TranslationProvider {

    private static final String PROVIDER_NAME = "DEEPL_WITH_GEMINI_FALLBACK";

    private final DeepLTranslationProvider primary;
    private final GeminiTranslationProvider fallback;

    public FailoverTranslationProvider(DeepLTranslationProvider primary, GeminiTranslationProvider fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public TranslationResult translate(String text, String targetLanguage) {
        try {
            return primary.translate(text, targetLanguage);
        } catch (Exception e) {
            log.warn("DeepL 번역 실패, Gemini로 failover. targetLanguage={}, reason={}", targetLanguage, e.getMessage());
            return fallback.translate(text, targetLanguage);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
