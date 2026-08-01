package com.deoham.chat.translation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    // ───────────────────────────────────────────────────────────────────────────
    // 서킷 브레이커
    // ───────────────────────────────────────────────────────────────────────────

    @Test
    void circuitOpens_afterConsecutiveDeeplFailures_thenSkipsDeeplStraightToGemini() {
        MutableClock clock = new MutableClock();
        provider = new FailoverTranslationProvider(primary, fallback,
                new SimpleCircuitBreaker(3, Duration.ofSeconds(30), clock));
        when(primary.translate("안녕", "en")).thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepL 다운"));
        when(fallback.translate("안녕", "en")).thenReturn(new TranslationResult("Hello", "GEMINI", "gemini-3.5-flash"));

        for (int i = 0; i < 3; i++) {
            assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("GEMINI");
        }
        verify(primary, times(3)).translate("안녕", "en"); // 3회 실패로 회로 오픈

        // 이후 요청은 DeepL을 건너뛰고 곧장 Gemini로 (primary 호출 증가 없음)
        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("GEMINI");
        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("GEMINI");
        verify(primary, times(3)).translate("안녕", "en");
    }

    @Test
    void circuitHalfOpens_afterCooldown_andRecoversToDeepl_whenProbeSucceeds() {
        MutableClock clock = new MutableClock();
        provider = new FailoverTranslationProvider(primary, fallback,
                new SimpleCircuitBreaker(1, Duration.ofSeconds(30), clock));
        when(primary.translate("안녕", "en"))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepL 다운")) // 최초 실패 → 오픈
                .thenReturn(new TranslationResult("Hello", "DEEPL", "DEEPL"));            // 탐침부터 성공
        when(fallback.translate("안녕", "en")).thenReturn(new TranslationResult("Hello", "GEMINI", "gemini-3.5-flash"));

        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("GEMINI"); // 1회 실패 → 오픈
        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("GEMINI"); // 쿨다운 중 → DeepL 건너뜀
        verify(primary, times(1)).translate("안녕", "en");

        clock.advance(Duration.ofSeconds(31));

        // 쿨다운 경과 → 탐침이 DeepL을 시도, 성공 → 회로 닫힘
        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("DEEPL");
        assertThat(provider.translate("안녕", "en").providerName()).isEqualTo("DEEPL");
        verify(primary, times(3)).translate("안녕", "en");
    }

    @Test
    void clientInputError_doesNotOpenCircuit() {
        MutableClock clock = new MutableClock();
        provider = new FailoverTranslationProvider(primary, fallback,
                new SimpleCircuitBreaker(1, Duration.ofSeconds(30), clock)); // 임계치 1
        when(primary.translate("안녕", "xx")).thenThrow(new BusinessException(ErrorCode.INVALID_REQUEST, "미지원 언어"));
        when(fallback.translate("안녕", "xx")).thenReturn(new TranslationResult("Hello", "GEMINI", "gemini-3.5-flash"));

        for (int i = 0; i < 3; i++) {
            assertThat(provider.translate("안녕", "xx").providerName()).isEqualTo("GEMINI");
        }
        // 400은 회로 실패로 세지 않으므로, 임계치 1이어도 매번 DeepL을 시도함
        verify(primary, times(3)).translate("안녕", "xx");
    }

    /** 테스트에서 시간을 수동으로 전진시키는 Clock. */
    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) {
            instant = instant.plus(d);
        }

        @Override public Instant instant() { return instant; }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public long millis() { return instant.toEpochMilli(); }
    }
}
