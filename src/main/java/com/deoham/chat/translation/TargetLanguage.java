package com.deoham.chat.translation;

import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 번역 대상 언어 화이트리스트.
 *
 * <p>클라이언트가 보낸 언어 코드는 반드시 이 enum을 거쳐야 하며, 그 뒤로는
 * <b>원문 문자열이 아니라 enum 값만</b> 흘러간다({@link TranslationProvider#translate} 시그니처가 이를 강제한다).
 * Gemini fallback 경로에서 언어 코드가 프롬프트의 <b>명령부</b>에 보간되기 때문에,
 * 자유 문자열을 허용하면 그 자체가 프롬프트 인젝션 통로가 된다.
 *
 * <p>프롬프트에 넣는 값은 원문이 아니라 {@link #displayName()}(고정 상수)이다.
 *
 * <p><b>지원 언어 목록은 잠정값이다.</b> DeepL 지원 집합 ∩ Gemini 처리 가능 집합을 어디까지 잡을지는
 * 기획 협의가 필요하다(이슈 #133의 "남은 결정 사항").
 */
public enum TargetLanguage {

    KO("ko", "Korean"),
    EN("en", "English"),
    JA("ja", "Japanese"),
    ZH_HANS("zh-hans", "Simplified Chinese"),
    ZH_HANT("zh-hant", "Traditional Chinese"),
    ES("es", "Spanish"),
    FR("fr", "French"),
    DE("de", "German"),
    VI("vi", "Vietnamese"),
    TH("th", "Thai");

    /** 오류 메시지에 되비추는 입력 길이 상한. 응답·로그로 원문이 길게 흘러나가지 않게 자른다. */
    private static final int MAX_ECHO_LENGTH = 20;

    private static final Map<String, TargetLanguage> BY_CODE = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(TargetLanguage::code, Function.identity()));

    private static final String SUPPORTED_CODES = Arrays.stream(values())
            .map(TargetLanguage::code)
            .collect(Collectors.joining(", "));

    private final String code;
    private final String displayName;

    TargetLanguage(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 클라이언트 입력을 화이트리스트 값으로 변환한다. 매칭되지 않으면 400으로 거부한다.
     *
     * <p>정규화는 소문자 + 하이픈 표기로 통일한다(예: {@code zh_Hans} → {@code zh-hans}).
     * 정확히 일치하는 코드가 없고 지역 서브태그가 붙어 있으면 기본 언어로 한 번 축약해 다시 찾는다
     * (예: {@code ko-KR} → {@code ko}, {@code en-US} → {@code en}). BCP 47 태그를 그대로 보내는
     * 클라이언트를 받아주기 위한 것으로, 어떤 경우든 결과는 이 enum 안의 값이다.
     */
    public static TargetLanguage from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw unsupported(raw);
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');

        TargetLanguage exact = BY_CODE.get(normalized);
        if (exact != null) {
            return exact;
        }

        int separator = normalized.indexOf('-');
        if (separator > 0) {
            TargetLanguage base = BY_CODE.get(normalized.substring(0, separator));
            if (base != null) {
                return base;
            }
        }
        throw unsupported(raw);
    }

    /**
     * 정규 언어 코드. 캐시 키({@code chat_message_translation.target_language})와 API 응답에 쓰인다.
     *
     * <p>표기를 소문자 하이픈으로 고른 이유는 두 가지다.
     * <ul>
     *   <li>기존 캐시 행이 {@code en}, {@code ko}처럼 소문자 코드로 저장되어 있어, 그대로 캐시 히트가 유지된다.
     *       enum 이름({@code EN}, {@code ZH_HANS})으로 저장하면 기존 행이 전부 미스가 난다.</li>
     *   <li>가장 긴 값이 {@code zh-hans}(7자)로 {@code VARCHAR(10)} 제약 안에 들어가 마이그레이션이 필요 없다.</li>
     * </ul>
     */
    public String code() {
        return code;
    }

    /**
     * 프롬프트에 삽입할 영어 언어명. 코드에 하드코딩된 고정 상수이므로
     * 사용자 입력이 프롬프트 명령부에 도달할 수 없다.
     */
    public String displayName() {
        return displayName;
    }

    private static BusinessException unsupported(String raw) {
        return new BusinessException(ErrorCode.INVALID_REQUEST,
                "지원하지 않는 번역 대상 언어입니다: " + echo(raw) + " (지원 언어: " + SUPPORTED_CODES + ")");
    }

    /**
     * 거부된 입력을 오류 메시지에 되비출 때 쓰는 정제 함수.
     * 제어문자(개행 등)를 제거해 로그 위조를 막고, 길이를 잘라 응답·로그 오염을 막는다.
     */
    private static String echo(String raw) {
        if (raw == null) {
            return "null";
        }
        String safe = raw.strip().replaceAll("[^\\p{Alnum}\\-_]", "?");
        return safe.length() > MAX_ECHO_LENGTH ? safe.substring(0, MAX_ECHO_LENGTH) + "..." : safe;
    }
}
