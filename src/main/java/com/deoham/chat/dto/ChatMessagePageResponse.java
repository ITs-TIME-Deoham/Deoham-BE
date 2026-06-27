package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "커서 기반 메시지 페이지 응답")
public record ChatMessagePageResponse(

        @Schema(description = "메시지 목록. 최신순(createdAt DESC)으로 정렬되어 있습니다.")
        List<ChatMessageResponse> messages,

        @Schema(
                description = "다음 페이지(이전 메시지) 존재 여부. `false`이면 가장 오래된 메시지까지 모두 조회한 상태입니다.",
                example = "true"
        )
        boolean hasNext,

        @Schema(
                description = """
                        다음 페이지 요청에 사용할 커서 값 (ISO-8601 UTC).
                        `hasNext`가 `true`일 때만 값이 있으며, 이 값을 `GET /messages?before=` 파라미터에 그대로 넣어 이전 메시지를 불러옵니다.
                        `hasNext`가 `false`이면 null입니다.
                        """,
                example = "2024-01-15T09:00:00Z",
                nullable = true
        )
        Instant nextCursor
) {
}
