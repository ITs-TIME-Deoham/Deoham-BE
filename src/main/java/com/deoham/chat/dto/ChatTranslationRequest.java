package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "메시지 번역 요청")
public record ChatTranslationRequest(

        @Schema(
                description = """
                        번역 대상 언어 코드 (BCP 47 / ISO 639-1 기반).
                        현재 지원 언어는 번역 제공자 설정에 따라 다릅니다.
                        일반적으로 사용되는 코드: `ko`(한국어), `en`(영어), `ja`(일본어), `zh`(중국어), `es`(스페인어)
                        """,
                example = "en"
        )
        @NotBlank String targetLanguage
) {
}
