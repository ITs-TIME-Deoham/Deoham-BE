package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "채팅 메시지")
public record ChatMessageResponse(

        @Schema(description = "메시지 UUID", example = "7c9e6679-7425-40de-944b-e07fc1f90ae7")
        UUID id,

        @Schema(description = "메시지가 속한 채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID roomId,

        @Schema(description = "발신자 사용자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID senderId,

        @Schema(description = "발신자 이름", example = "홍길동")
        String senderName,

        @Schema(
                description = "메시지 타입",
                example = "TEXT",
                allowableValues = {"TEXT", "IMAGE", "FILE"}
        )
        String messageType,

        @Schema(
                description = "텍스트 메시지 내용. `messageType`이 `TEXT`가 아니면 null.",
                example = "안녕하세요!",
                nullable = true
        )
        String content,

        @Schema(
                description = "첨부파일 S3 key. `messageType`이 `IMAGE` 또는 `FILE`일 때 값이 있습니다.",
                example = "chat/550e8400-e29b-41d4-a716-446655440000/f47ac10b_profile.png",
                nullable = true
        )
        String attachmentUrl,

        @Schema(
                description = "첨부파일 원본 파일명",
                example = "profile.png",
                nullable = true
        )
        String attachmentFileName,

        @Schema(
                description = "첨부파일 MIME 타입",
                example = "image/png",
                nullable = true
        )
        String attachmentContentType,

        @Schema(
                description = "첨부파일 크기 (bytes)",
                example = "204800",
                nullable = true
        )
        Long attachmentSizeBytes,

        @Schema(description = "메시지 전송 시각 (ISO-8601 UTC). 커서 기반 페이지네이션의 커서 값으로 사용됩니다.", example = "2024-01-15T10:30:00Z")
        Instant createdAt
) {
}
