package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "채팅방 정보")
public record ChatRoomResponse(

        @Schema(description = "채팅방 UUID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(
                description = "연결된 프로젝트 UUID. 프로젝트에 묶이지 않은 채팅방은 null.",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                nullable = true
        )
        UUID projectId,

        @Schema(
                description = "채팅방 이름. 다이렉트(1:1) 채팅방은 항상 null — 클라이언트가 상대방 이름을 표시해야 합니다.",
                example = "프로젝트 A 팀 채팅",
                nullable = true
        )
        String name,

        @Schema(
                description = "1:1 다이렉트 채팅방 여부. `members` 크기가 2이고 이 값이 true이면 DM 채팅방입니다.",
                example = "false"
        )
        boolean isDirect,

        @Schema(description = "현재 방에 참여 중인 멤버 목록 (나간 멤버 제외)")
        List<ChatRoomMemberResponse> members,

        @Schema(
                description = "마지막 메시지 전송 시각. 아직 메시지가 없으면 null.",
                example = "2024-01-15T10:30:00Z",
                nullable = true
        )
        Instant lastMessageAt,

        @Schema(description = "채팅방 생성 시각 (ISO-8601 UTC)", example = "2024-01-01T00:00:00Z")
        Instant createdAt
) {
}
