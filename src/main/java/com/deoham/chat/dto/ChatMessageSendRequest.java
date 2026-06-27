package com.deoham.chat.dto;

import com.deoham.chat.entity.ChatMessageType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "메시지 전송 요청")
public record ChatMessageSendRequest(

        @Schema(
                description = """
                        메시지 타입.
                        - `TEXT`: 텍스트 메시지 — `content` 필드 필수
                        - `IMAGE`: 이미지 첨부 — `attachmentUrl`, `attachmentFileName`, `attachmentContentType` 필수
                        - `FILE`: 파일 첨부 — `attachmentUrl`, `attachmentFileName`, `attachmentContentType`, `attachmentSizeBytes` 필수
                        """,
                example = "TEXT"
        )
        @NotNull ChatMessageType messageType,

        @Schema(
                description = "텍스트 메시지 내용. `messageType`이 `TEXT`일 때 필수, 나머지 타입은 null로 보냅니다.",
                example = "안녕하세요!",
                nullable = true
        )
        String content,

        @Schema(
                description = """
                        첨부파일의 S3 key. `POST /attachments/presign` 응답의 `attachmentUrl` 값을 그대로 사용합니다.
                        `messageType`이 `IMAGE` 또는 `FILE`일 때 필수.
                        """,
                example = "chat/550e8400-e29b-41d4-a716-446655440000/f47ac10b_profile.png",
                nullable = true
        )
        String attachmentUrl,

        @Schema(
                description = "첨부파일 원본 파일명. `IMAGE`, `FILE` 타입일 때 필수.",
                example = "profile.png",
                nullable = true
        )
        String attachmentFileName,

        @Schema(
                description = "첨부파일 MIME 타입. `IMAGE`, `FILE` 타입일 때 필수.",
                example = "image/png",
                nullable = true
        )
        String attachmentContentType,

        @Schema(
                description = "첨부파일 크기 (bytes). `FILE` 타입일 때 필수. `IMAGE`는 선택 값.",
                example = "204800",
                nullable = true
        )
        Long attachmentSizeBytes
) {
}
