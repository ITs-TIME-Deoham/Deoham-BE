package com.deoham.chat.controller;

import com.deoham.chat.dto.ChatAttachmentPresignRequest;
import com.deoham.chat.dto.ChatAttachmentPresignResponse;
import com.deoham.chat.dto.ChatMessagePageResponse;
import com.deoham.chat.dto.ChatMessageResponse;
import com.deoham.chat.dto.ChatMessageSendRequest;
import com.deoham.chat.service.ChatAttachmentService;
import com.deoham.chat.service.ChatMessageService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.SupabaseAuthenticationUtils;
import com.deoham.global.security.SupabasePrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "채팅 메시지",
        description = """
                채팅 메시지 전송·조회 및 첨부파일 업로드 URL 발급 API.

                **실시간 메시지 전송 (WebSocket/STOMP)**
                WebSocket을 사용할 수 있는 환경에서는 STOMP 프로토콜을 이용한 실시간 전송을 권장합니다.
                - 연결: `ws://{host}/ws/chat` (SockJS 폴백 지원)
                - 구독: `SUBSCRIBE /sub/chat/rooms/{roomId}` — 해당 방의 실시간 메시지를 수신
                - 발행: `SEND /pub/chat/rooms/{roomId}/messages` — 메시지 전송 (body: `ChatMessageSendRequest` JSON)
                - JWT 인증: STOMP CONNECT 프레임의 `Authorization` 헤더에 `Bearer {token}` 을 포함합니다.

                REST API(`POST /messages`)는 WebSocket을 사용할 수 없는 환경을 위한 폴백입니다.
                REST 전송 시에는 실시간 구독자에게 별도 알림이 전달되지 않을 수 있습니다.
                """
)
@RestController
@RequestMapping("/api/chat/rooms/{roomId}")
@RequiredArgsConstructor
public class ChatMessageController {

    private final ChatMessageService chatMessageService;
    private final ChatAttachmentService chatAttachmentService;

    @Operation(
            summary = "메시지 전송 (REST 폴백)",
            description = """
                    채팅방에 메시지를 전송합니다. 실시간 연결이 불가한 환경에서 사용하는 REST 폴백 엔드포인트입니다.

                    **messageType 별 필수 필드**
                    | messageType | 필수 | 불필요 |
                    |---|---|---|
                    | `TEXT` | `content` | attachment 필드 전체 |
                    | `IMAGE` | `attachmentUrl`, `attachmentFileName`, `attachmentContentType` | `content` |
                    | `FILE` | `attachmentUrl`, `attachmentFileName`, `attachmentContentType`, `attachmentSizeBytes` | `content` |

                    첨부파일(`IMAGE`, `FILE`) 전송 순서:
                    1. `POST /attachments/presign` 으로 S3 Presigned URL 발급
                    2. 클라이언트가 Presigned URL로 파일을 직접 `PUT` 업로드
                    3. 발급받은 `attachmentUrl`(S3 key)을 포함해 이 엔드포인트 호출

                    전송 성공 시 같은 방의 다른 멤버에게 푸시 알림이 발송됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 전송 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "유효성 오류 — TEXT인데 `content` 없음, 또는 첨부 메시지인데 `attachmentUrl` 없음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "INVALID_REQUEST", "message": "텍스트 메시지는 content가 필요합니다" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "FORBIDDEN", "message": "채팅방 멤버가 아닙니다" }
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @Parameter(description = "메시지를 전송할 채팅방 UUID") @PathVariable UUID roomId,
            @Valid @RequestBody ChatMessageSendRequest request) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatMessageService.sendMessage(roomId, userId, request));
    }

    @Operation(
            summary = "메시지 목록 조회 (커서 기반 페이지네이션)",
            description = """
                    채팅방의 메시지를 최신순(내림차순)으로 조회합니다. `before` 커서 기반 페이지네이션을 사용합니다.

                    **최초 조회 (최신 메시지부터)**
                    `before` 없이 요청합니다.
                    ```
                    GET /api/chat/rooms/{roomId}/messages?size=30
                    ```

                    **이전 메시지 더 불러오기**
                    응답의 `nextCursor` 값을 `before` 파라미터에 넣어 다음 페이지를 요청합니다.
                    ```
                    GET /api/chat/rooms/{roomId}/messages?before=2024-01-15T10:30:00Z&size=30
                    ```

                    응답의 `hasNext`가 `false`이면 더 이상 이전 메시지가 없습니다.

                    **정렬**: 항상 `createdAt DESC` (최신 메시지가 배열 앞쪽)
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 목록 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "FORBIDDEN", "message": "채팅방 멤버가 아닙니다" }
                                    }
                                    """
                    )
            )
    )
    @GetMapping("/messages")
    public ApiResponse<ChatMessagePageResponse> getMessages(
            @Parameter(description = "조회할 채팅방 UUID") @PathVariable UUID roomId,
            @Parameter(
                    description = "커서 — 이 시각 이전의 메시지를 조회합니다. ISO-8601 형식 (예: `2024-01-15T10:30:00Z`). 생략하면 최신 메시지부터 조회합니다.",
                    example = "2024-01-15T10:30:00Z"
            )
            @RequestParam(required = false) Instant before,
            @Parameter(description = "한 번에 조회할 메시지 수 (기본값: 30)", example = "30")
            @RequestParam(defaultValue = "30") int size) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatMessageService.getMessages(userId, roomId, before, size));
    }

    @Operation(
            summary = "첨부파일 S3 Presigned Upload URL 발급",
            description = """
                    채팅 첨부파일(이미지·파일)을 S3에 직접 업로드하기 위한 Presigned PUT URL을 발급합니다.

                    **업로드 흐름**
                    1. 이 엔드포인트를 호출해 `uploadUrl`과 `attachmentUrl`(S3 key)을 받습니다.
                    2. 클라이언트가 `uploadUrl`로 `HTTP PUT` 요청을 보내 파일을 직접 업로드합니다.
                       - `Content-Type` 헤더를 요청 시 지정한 `contentType`과 동일하게 설정해야 합니다.
                    3. 업로드 성공 후, `attachmentUrl`(S3 key)과 파일 정보를 담아 메시지 전송 API를 호출합니다.

                    **Presigned URL 유효 시간**: 서버 환경변수 `AWS_S3_PRESIGNED_TTL` 설정값을 따릅니다.

                    **S3 key 형식**: `chat/{roomId}/{uuid}_{fileName}`
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "유효성 오류 — `fileName` 또는 `contentType` 누락",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "INVALID_REQUEST", "message": "..." }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "UNAUTHORIZED", "message": "Authentication required" }
                                    }
                                    """
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "채팅방 멤버가 아님",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "FORBIDDEN", "message": "채팅방 멤버가 아닙니다" }
                                    }
                                    """
                    )
            )
    )
    @PostMapping("/attachments/presign")
    public ApiResponse<ChatAttachmentPresignResponse> presignAttachment(
            @Parameter(description = "첨부파일을 업로드할 채팅방 UUID") @PathVariable UUID roomId,
            @Valid @RequestBody ChatAttachmentPresignRequest request) {
        UUID userId = currentUserId();
        return ApiResponse.ok(chatAttachmentService.createUploadUrl(roomId, userId, request));
    }

    private UUID currentUserId() {
        SupabasePrincipal principal = SupabaseAuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        return principal.userId();
    }
}
