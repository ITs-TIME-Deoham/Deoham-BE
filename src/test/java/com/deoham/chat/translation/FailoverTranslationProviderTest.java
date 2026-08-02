package com.deoham.chat.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FailoverTranslationProviderTest {

    @Mock
    private DeepLTranslationProvider primary;
    @Mock
    private GeminiTranslationProvider fallback;

    private FailoverTranslationProvider provider;

    @BeforeEach
    void setUp() {
        provider = new FailoverTranslationProvider(primary, fallback);
    }

    @Test
    void translate_usesPrimaryResult_whenPrimarySucceeds() {
        when(primary.translate("안녕", "en")).thenReturn(new TranslationResult("Hello", "DEEPL", "DEEPL"));

        TranslationResult result = provider.translate("안녕", "en");

        assertThat(result.translatedText()).isEqualTo("Hello");
        assertThat(result.providerName()).isEqualTo("DEEPL");
        verifyNoInteractions(fallback);
    }

    @Test
    void translate_fallsBackToGemini_whenPrimaryThrows() {
        when(primary.translate("안녕", "en")).thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepL 오류"));
        when(fallback.translate("안녕", "en")).thenReturn(new TranslationResult("Hello", "GEMINI", "gemini-3.5-flash"));

        TranslationResult result = provider.translate("안녕", "en");

        assertThat(result.translatedText()).isEqualTo("Hello");
        assertThat(result.providerName()).isEqualTo("GEMINI");
        assertThat(result.modelVersion()).isEqualTo("gemini-3.5-flash");
    }

    @Test
    void translate_propagatesFallbackException_whenBothFail() {
        when(primary.translate("안녕", "en")).thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepL 오류"));
        when(fallback.translate("안녕", "en")).thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "Gemini 오류"));

        assertThatThrownBy(() -> provider.translate("안녕", "en"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Gemini 오류");
    }

    @Test
    void getProviderName_returnsCompositeName() {
        assertThat(provider.getProviderName()).isEqualTo("DEEPL_WITH_GEMINI_FALLBACK");
    }
}
