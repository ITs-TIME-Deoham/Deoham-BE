package com.deoham.chat.controller.docs;

import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;

@Tag(
        name = "채팅 메시지",
        description = """
                채팅 메시지 전송·조회 API.

                **실시간 전송 (WebSocket/STOMP)**
                - 연결: `ws://{host}/ws/chat`
                - 구독: `SUBSCRIBE /sub/chat/rooms/{roomId}`
                - 발행: `SEND /pub/chat/rooms/{roomId}/messages` (body: ChatMessageSendRequest JSON)
                - 인증: STOMP CONNECT 프레임 `Authorization: Bearer {token}`

                REST `POST /messages`는 WebSocket 미지원 환경을 위한 폴백입니다.
                """
)
public interface ChatMessageControllerDocs {

    @Operation(
            summary = "메시지 전송 (REST 폴백)",
            description = """
                    채팅방에 메시지를 전송합니다.

                    | messageType | content |
                    |---|---|
                    | `TEXT` | 텍스트 본문 |
                    | `IMAGE` | 이미지 URL |
                    | `LOCATION` | 위치 데이터 (예: JSON 문자열) |
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효성 오류",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"INVALID_REQUEST","message":"..."}}
                            """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<ChatMessageResponse> sendMessage(
            @Parameter(description = "메시지를 전송할 채팅방 UUID") UUID roomId,
            @Valid ChatMessageSendRequest request);

    @Operation(
            summary = "메시지 목록 조회 (커서 기반)",
            description = """
                    채팅방 메시지를 최신순(sentAt DESC)으로 조회합니다.

                    - 최초 조회: `before` 파라미터 생략
                    - 이전 메시지 더보기: 응답의 `nextCursor` 값을 `before`에 전달
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<ChatMessagePageResponse> getMessages(
            @Parameter(description = "채팅방 UUID") UUID roomId,
            @Parameter(description = "커서 — 이 시각 이전의 메시지 조회 (ISO-8601). 생략 시 최신부터", example = "2024-01-15T10:30:00Z") Instant before,
            @Parameter(description = "조회할 메시지 수 (기본값: 30)", example = "30") int size);
}
