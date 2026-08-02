package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;

public interface TranslationProvider {

    TranslationResult translate(String text, String targetLanguage);

    String getProviderName();
}
