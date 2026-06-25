package com.deoham.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatTranslationRequest(
        @NotBlank String targetLanguage
) {
}
