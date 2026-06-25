package com.deoham.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatTranslationResponse(
        UUID messageId,
        String targetLanguage,
        String translatedText,
        boolean cached,
        Instant translatedAt
) {
}
