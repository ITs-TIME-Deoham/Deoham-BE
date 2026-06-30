package com.deoham.chat.translation;

public interface TranslationProvider {

    TranslationResult translate(String text, String targetLanguage);

    String getProviderName();
}
