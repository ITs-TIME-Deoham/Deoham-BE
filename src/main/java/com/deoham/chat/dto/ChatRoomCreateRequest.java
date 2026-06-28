package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Schema(description = "채팅방 생성/조회 요청")
public record ChatRoomCreateRequest(

        @Schema(
                description = "채팅방을 연결할 카드 UUID. 카드 1개에 채팅방 1개가 생성됩니다.",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        )
        @NotNull UUID cardId
) {
}
