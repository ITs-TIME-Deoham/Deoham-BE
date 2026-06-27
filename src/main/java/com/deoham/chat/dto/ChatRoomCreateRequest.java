package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

@Schema(description = "채팅방 생성 요청")
public record ChatRoomCreateRequest(

        @Schema(
                description = "채팅방을 연결할 프로젝트 UUID. 특정 프로젝트에 묶인 채팅방이 아닌 경우 null로 보냅니다.",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479",
                nullable = true
        )
        UUID projectId,

        @Schema(
                description = """
                        초대할 멤버의 사용자 UUID 목록 (요청자 본인 제외).
                        크기가 1이면 1:1 다이렉트 채팅방으로 생성됩니다.
                        크기가 2 이상이면 그룹 채팅방으로 생성됩니다.
                        """,
                example = "[\"a1b2c3d4-e5f6-7890-abcd-ef1234567890\"]"
        )
        @NotEmpty List<UUID> memberUserIds,

        @Schema(
                description = "채팅방 이름. 다이렉트 채팅방은 무시됩니다(항상 null 저장). 그룹 채팅방에서 null 전달 시 이름 없이 생성됩니다.",
                example = "프로젝트 A 팀 채팅",
                nullable = true
        )
        String name
) {
}
