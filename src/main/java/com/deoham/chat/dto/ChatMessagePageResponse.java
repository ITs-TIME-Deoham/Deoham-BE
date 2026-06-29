package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 메시지 페이지 응답")
public record ChatMessagePageResponse(

        @Schema(description = "메시지 목록. 최신순(sentAt DESC)으로 정렬됩니다.")
        List<ChatMessageResponse> messages,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(
                description = "다음 페이지 요청에 사용할 커서(sentAt). hasNext가 false이면 null.",
                example = "2024-01-15T09:00:00Z",
                nullable = true
        )
        Instant nextCursor
) {
}
