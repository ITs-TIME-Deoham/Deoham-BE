package com.deoham.chat.dto;

import java.time.Instant;
import java.util.List;

public record ChatMessagePageResponse(
        List<ChatMessageResponse> messages,
        boolean hasNext,
        Instant nextCursor
) {
}
