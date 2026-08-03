package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "메시지 번역 요청")
public record ChatTranslationRequest(

        @Schema(
                description = """
                        번역 대상 언어 코드 (BCP 47 / ISO 639-1 기반).
                        지원 언어: `ko`, `en`, `ja`, `zh-hans`, `zh-hant`, `es`, `fr`, `de`, `vi`, `th`.
                        지역 서브태그가 붙은 코드(`ko-KR`, `en-US`)는 기본 언어로 축약되어 처리됩니다.
                        목록에 없는 값은 외부 번역 API를 호출하지 않고 400으로 거부됩니다.
                        """,
                example = "en",
                allowableValues = {"ko", "en", "ja", "zh-hans", "zh-hant", "es", "fr", "de", "vi", "th"}
        )
        // 최소 방어선. 실제 허용 집합은 TargetLanguage 화이트리스트가 결정하지만,
        // 이 패턴이 "언어 코드처럼 생기지 않은 값"(자연어 문장, 개행, 초장문)을 먼저 걷어낸다.
        // Bean Validation 의 @Pattern 은 Matcher#matches() 로 검사하므로 문자열 전체가 앵커링된다
        // ("en\n" 처럼 개행이 붙은 값도 통과하지 못한다).
        @NotBlank
        @Pattern(
                regexp = "^[a-zA-Z]{2,3}(-[a-zA-Z0-9]{2,4})?$",
                message = "언어 코드 형식이 올바르지 않습니다 (예: en, ko, zh-hans)"
        )
        String targetLanguage
) {
}
