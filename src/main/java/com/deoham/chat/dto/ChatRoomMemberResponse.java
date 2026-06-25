package com.deoham.chat.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatRoomMemberResponse(
        UUID userId,
        String userName,
        String role,
        Instant joinedAt
) {
}
