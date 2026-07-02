package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "메시지 읽음 이벤트 (WebSocket 브로드캐스트 전용)")
public record ChatReadEvent(

        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID roomId,

        @Schema(description = "메시지를 읽은 사용자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID readerId,

        @Schema(description = "이번에 읽음 처리된 메시지 UUID 목록")
        List<UUID> messageIds,

        @Schema(description = "읽음 처리 시각 (ISO-8601 UTC)", example = "2024-01-15T10:31:00Z")
        Instant readAt
) {
}
