package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "첨부파일 S3 Presigned Upload URL 발급 요청")
public record ChatAttachmentPresignRequest(

        @Schema(
                description = "업로드할 파일의 원본 파일명. S3 key에 포함되며, 메시지 전송 시 `attachmentFileName`으로 그대로 사용합니다.",
                example = "profile.png"
        )
        @NotBlank String fileName,

        @Schema(
                description = """
                        업로드할 파일의 MIME 타입.
                        S3 Presigned URL 서명에 포함되므로, 실제 S3 PUT 요청 시 `Content-Type` 헤더를 이 값과 동일하게 설정해야 합니다.
                        """,
                example = "image/png"
        )
        @NotBlank String contentType
) {
}
