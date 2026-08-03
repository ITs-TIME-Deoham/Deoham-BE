package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;

public interface TranslationProvider {

    /**
     * 대상 언어를 {@link TargetLanguage}로 받는다. 자유 문자열이 아니라 화이트리스트 타입이므로,
     * 검증되지 않은 클라이언트 입력이 provider 내부(특히 Gemini 프롬프트)로 흘러들어갈 수 없다.
     */
    TranslationResult translate(String text, TargetLanguage targetLanguage);

    String getProviderName();
}
