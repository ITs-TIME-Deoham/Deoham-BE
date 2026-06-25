package com.deoham.chat.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatRoomResponse(
        UUID id,
        UUID projectId,
        String name,
        boolean isDirect,
        List<ChatRoomMemberResponse> members,
        Instant lastMessageAt,
        Instant createdAt
) {
}
