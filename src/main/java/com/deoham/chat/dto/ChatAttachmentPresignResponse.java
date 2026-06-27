package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "첨부파일 S3 Presigned Upload URL 발급 응답")
public record ChatAttachmentPresignResponse(

        @Schema(
                description = """
                        S3 Presigned PUT URL. 클라이언트는 이 URL로 `HTTP PUT` 요청을 보내 파일을 직접 업로드합니다.
                        유효 시간 내에만 사용 가능하며, 만료 시 재발급이 필요합니다.
                        이 URL을 메시지 전송 요청에 포함하지 마세요 — `attachmentUrl` 필드를 사용합니다.
                        """,
                example = "https://bucket.s3.amazonaws.com/chat/roomId/uuid_profile.png?X-Amz-Signature=..."
        )
        String uploadUrl,

        @Schema(
                description = """
                        파일의 S3 key. 형식: `chat/{roomId}/{uuid}_{fileName}`.
                        S3 PUT 업로드 완료 후, 메시지 전송 요청(`ChatMessageSendRequest`)의 `attachmentUrl` 필드에 이 값을 그대로 사용합니다.
                        """,
                example = "chat/550e8400-e29b-41d4-a716-446655440000/f47ac10b-58cc-4372-a567-0e02b2c3d479_profile.png"
        )
        String attachmentUrl
) {
}
