package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "메시지 번역 결과")
public record ChatTranslationResponse(

        @Schema(description = "번역된 원본 메시지 UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID messageId,

        @Schema(description = "번역 대상 언어 코드", example = "en")
        String targetLanguage,

        @Schema(description = "번역된 텍스트", example = "Hello! How are you?")
        String translatedText,

        @Schema(
                description = "캐시 적중 여부. `true`이면 이전에 동일한 (messageId, targetLanguage) 조합으로 번역된 결과를 반환한 것입니다.",
                example = "false"
        )
        boolean cached,

        @Schema(
                description = "번역이 저장(또는 캐시 기록)된 시각 (ISO-8601 UTC)",
                example = "2024-01-15T10:35:00Z"
        )
        Instant translatedAt
) {
}
