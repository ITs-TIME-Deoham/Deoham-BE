package com.deoham.chat.translation;

import org.springframework.stereotype.Component;

/**
 * Test-only stand-in for GeminiTranslationProvider (excluded from the "test" profile there),
 * so tests don't hit the real Gemini API.
 */
@Component
public class DummyTranslationProvider implements TranslationProvider {

    @Override
    public TranslationResult translate(String text, String targetLanguage) {
        return new TranslationResult("[%s] %s".formatted(targetLanguage.toUpperCase(), text), "DUMMY", "dummy-v1");
    }

    @Override
    public String getProviderName() {
        return "DUMMY";
    }
}
