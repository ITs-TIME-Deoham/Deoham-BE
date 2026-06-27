package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "채팅방 멤버 정보")
public record ChatRoomMemberResponse(

        @Schema(description = "멤버의 사용자 UUID", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        UUID userId,

        @Schema(description = "멤버의 사용자 이름", example = "홍길동")
        String userName,

        @Schema(
                description = "채팅방 내 역할. `OWNER` — 방 개설자, `MEMBER` — 일반 멤버.",
                example = "OWNER",
                allowableValues = {"OWNER", "MEMBER"}
        )
        String role,

        @Schema(description = "채팅방 참여(입장) 시각 (ISO-8601 UTC)", example = "2024-01-01T00:00:00Z")
        Instant joinedAt
) {
}
