package com.deoham.chat.dto;

import com.deoham.chat.entity.ChatMessageType;
import jakarta.validation.constraints.NotNull;

public record ChatMessageSendRequest(
        @NotNull ChatMessageType messageType,
        String content,
        String attachmentUrl,
        String attachmentFileName,
        String attachmentContentType,
        Long attachmentSizeBytes
) {
}
