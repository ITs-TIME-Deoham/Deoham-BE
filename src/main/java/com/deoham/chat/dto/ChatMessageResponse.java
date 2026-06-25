package com.deoham.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatMessageResponse(
        UUID id,
        UUID roomId,
        UUID senderId,
        String senderName,
        String messageType,
        String content,
        String attachmentUrl,
        String attachmentFileName,
        String attachmentContentType,
        Long attachmentSizeBytes,
        Instant createdAt
) {
}
