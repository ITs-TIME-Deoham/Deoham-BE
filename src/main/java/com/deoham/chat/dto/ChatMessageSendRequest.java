package com.deoham.chat.dto;

import com.deoham.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "메시지 전송 요청")
public record ChatMessageSendRequest(

        @Schema(
                description = """
                        메시지 타입.
                        - `TEXT`: 텍스트 — `content`에 본문 입력
                        - `IMAGE`: 이미지 — `content`에 이미지 URL 입력
                        - `LOCATION`: 위치 — `content`에 위치 정보(예: JSON) 입력
                        """,
                example = "TEXT"
        )
        @NotNull ChatMessageType messageType,

        @Schema(
                description = "메시지 내용. TEXT면 본문, IMAGE면 이미지 URL, LOCATION이면 위치 데이터.",
                example = "안녕하세요!"
        )
        @NotBlank String content
) {
}
