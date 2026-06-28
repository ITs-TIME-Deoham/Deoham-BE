package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "채팅방 정보")
public record ChatRoomResponse(

        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "연결된 카드 UUID", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
        UUID cardId,

        @Schema(description = "채팅방 상태", example = "ACTIVE", allowableValues = {"ACTIVE", "CLOSED"})
        String status,

        @Schema(description = "채팅방 생성 시각 (ISO-8601 UTC)", example = "2024-01-01T00:00:00Z")
        Instant createdAt,

        @Schema(description = "채팅방 종료 시각. ACTIVE 상태이면 null.", example = "2024-01-02T00:00:00Z", nullable = true)
        Instant closedAt
) {
}
