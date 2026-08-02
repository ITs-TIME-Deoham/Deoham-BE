package com.deoham.chat.translation;

import com.deoham.chat.translation.dto.TranslationResult;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * DeepL을 우선 시도하고, 실패(네트워크 오류·인증 오류·미지원 언어 등 어떤 이유든)하면
 * Gemini로 자동 전환한다. 어느 provider가 실제로 처리했는지는 {@link TranslationResult#providerName()}에
 * 담겨 호출자에게 그대로 전달된다.
 *
 * <p>DeepL이 지속적으로 실패하면 매 요청마다 DeepL 타임아웃을 다시 지불하는 낭비가 생기므로,
 * {@link SimpleCircuitBreaker}로 DeepL을 보호한다. 회로가 열리면 DeepL을 건너뛰고 곧바로 Gemini로 간다.
 * 미지원 언어 같은 <b>클라이언트 입력 오류</b>(400)는 DeepL 자체는 살아있다는 신호이므로 회로 실패로 세지 않는다.
 */
@Slf4j
@Primary
@Profile("!test")
@Component
public class FailoverTranslationProvider implements TranslationProvider {

    private static final String PROVIDER_NAME = "DEEPL_WITH_GEMINI_FALLBACK";

    /** 연속 실패 이 횟수에 도달하면 DeepL 회로를 연다. */
    private static final int FAILURE_THRESHOLD = 3;
    /** 회로가 열린 뒤 이 시간이 지나야 DeepL 탐침을 1건 허용한다. */
    private static final Duration OPEN_DURATION = Duration.ofSeconds(30);

    private final DeepLTranslationProvider primary;
    private final GeminiTranslationProvider fallback;
    private final SimpleCircuitBreaker deeplBreaker;

    public FailoverTranslationProvider(DeepLTranslationProvider primary, GeminiTranslationProvider fallback) {
        this(primary, fallback, new SimpleCircuitBreaker(FAILURE_THRESHOLD, OPEN_DURATION, Clock.systemUTC()));
    }

    // 테스트에서 Clock/임계치를 제어한 브레이커를 주입하기 위한 생성자
    FailoverTranslationProvider(DeepLTranslationProvider primary, GeminiTranslationProvider fallback,
            SimpleCircuitBreaker deeplBreaker) {
        this.primary = primary;
        this.fallback = fallback;
        this.deeplBreaker = deeplBreaker;
    }

    @Override
    public TranslationResult translate(String text, String targetLanguage) {
        if (deeplBreaker.tryAcquire()) {
            try {
                TranslationResult result = primary.translate(text, targetLanguage);
                deeplBreaker.onSuccess();
                return result;
            } catch (Exception e) {
                if (isDeeplHealthFailure(e)) {
                    deeplBreaker.onFailure();
                } else {
                    // 400(미지원 언어) 등: DeepL은 응답했으므로 건강한 것으로 보고 이 요청만 Gemini로 넘긴다.
                    deeplBreaker.onSuccess();
                }
                log.warn("DeepL 번역 실패, Gemini로 failover. targetLanguage={}, reason={}", targetLanguage, e.getMessage());
            }
        } else {
            log.debug("DeepL 회로 OPEN, DeepL 건너뛰고 Gemini로 직행. targetLanguage={}", targetLanguage);
        }
        return fallback.translate(text, targetLanguage);
    }

    /**
     * DeepL 실패가 <b>다운스트림 건강 문제</b>(타임아웃·연결 오류·인증/쿼터/5xx 등)인지 판단한다.
     * 클라이언트 입력 문제({@link ErrorCode#INVALID_REQUEST}, 예: 미지원 언어)는 DeepL 자체 장애가 아니므로 제외한다.
     */
    private static boolean isDeeplHealthFailure(Exception e) {
        if (e instanceof BusinessException be) {
            return be.getErrorCode() != ErrorCode.INVALID_REQUEST;
        }
        return true; // ResourceAccessException 등 전송 계층 실패
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
