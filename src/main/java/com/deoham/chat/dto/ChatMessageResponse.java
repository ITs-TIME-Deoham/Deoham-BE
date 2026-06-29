package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "채팅 메시지")
public record ChatMessageResponse(

        @Schema(description = "메시지 UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "메시지가 속한 채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID chatRoomId,

        @Schema(description = "발신자 사용자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID senderId,

        @Schema(description = "발신자 닉네임", example = "홍길동")
        String senderNickname,

        @Schema(
                description = "메시지 타입",
                example = "TEXT",
                allowableValues = {"TEXT", "IMAGE", "LOCATION"}
        )
        String messageType,

        @Schema(description = "메시지 내용. TEXT면 본문, IMAGE면 이미지 URL, LOCATION이면 위치 데이터.", example = "안녕하세요!")
        String content,

        @Schema(description = "메시지 전송 시각 (ISO-8601 UTC)", example = "2024-01-15T10:30:00Z")
        Instant sentAt,

        @Schema(description = "읽은 시각. 아직 읽지 않았으면 null.", example = "2024-01-15T10:31:00Z", nullable = true)
        Instant readAt
) {
}
