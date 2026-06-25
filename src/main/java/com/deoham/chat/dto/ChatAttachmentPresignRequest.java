package com.deoham.chat.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatAttachmentPresignRequest(
        @NotBlank String fileName,
        @NotBlank String contentType
) {
}
