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
                - 연결: `ws://{host}/ws`
                - 구독(메시지 수신): `SUBSCRIBE /sub/chat/rooms/{roomId}`
                - 발행: `SEND /pub/chat/rooms/{roomId}/messages` (body: ChatMessageSendRequest JSON)
                - 인증: STOMP CONNECT 프레임 `Authorization: Bearer {token}`

                REST `POST /messages`는 WebSocket 미지원 환경을 위한 폴백입니다.

                **읽음 이벤트 실시간 브로드캐스트**
                - 구독: `SUBSCRIBE /sub/chat/rooms/{roomId}/read`
                - `PATCH /messages/read` 호출로 메시지가 읽음 처리되면, 해당 채팅방을 구독 중인 상대방에게
                  `ChatReadEvent`(roomId, readerId, messageIds, readAt)가 즉시 push됩니다.
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

    @Operation(
            summary = "메시지 읽음 처리",
            description = """
                    채팅방에서 상대방이 보낸 메시지 중 아직 읽지 않은 메시지를 모두 읽음 처리합니다.

                    내가 보낸 메시지는 대상에서 제외됩니다. 채팅방 화면 진입 시 호출하는 것을 권장합니다.

                    읽음 처리된 메시지가 있으면 `/sub/chat/rooms/{roomId}/read`를 구독 중인 상대방에게
                    `ChatReadEvent`가 실시간으로 브로드캐스트됩니다. 읽을 메시지가 없으면 브로드캐스트하지 않습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공 (data: null)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<Void> markMessagesAsRead(@Parameter(description = "채팅방 UUID") UUID roomId);
}
