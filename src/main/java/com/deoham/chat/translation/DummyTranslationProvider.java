package com.deoham.chat.translation;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Placeholder until a real provider (AWS Translate / OpenAI / Anthropic) is wired in.
 */
@Primary
@Component
public class DummyTranslationProvider implements TranslationProvider {

    @Override
    public TranslationResult translate(String text, String targetLanguage) {
        return new TranslationResult("[%s] %s".formatted(targetLanguage.toUpperCase(), text), "dummy-v1");
    }

    @Override
    public String getProviderName() {
        return "DUMMY";
    }
}
