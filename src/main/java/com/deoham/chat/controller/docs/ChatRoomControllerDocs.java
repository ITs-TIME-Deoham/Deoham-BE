package com.deoham.chat.controller.docs;

import com.deoham.chat.dto.ChatRoomCreateRequest;
import com.deoham.chat.dto.ChatRoomResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Tag(name = "채팅방", description = "채팅방 생성·조회·나가기 API")
public interface ChatRoomControllerDocs {

    @Operation(
            summary = "채팅방 생성",
            description = """
                    새로운 채팅방을 생성합니다.

                    **멤버 구성**
                    - 요청자(본인)는 자동으로 `OWNER` 역할로 추가됩니다.
                    - `memberUserIds`에 포함된 사용자들은 `MEMBER` 역할로 추가됩니다.

                    **다이렉트(1:1) 채팅방**
                    - `memberUserIds`의 크기가 1이면 1:1 다이렉트 채팅방(`isDirect = true`)으로 생성됩니다.
                    - 두 사용자 간 다이렉트 방이 이미 존재하면 새로 만들지 않고 기존 방을 반환합니다(멱등성).

                    **채팅방 이름(`name`)**
                    - 다이렉트 채팅방은 `name`이 항상 null입니다 — 클라이언트가 상대방 이름을 표시해야 합니다.
                    - 그룹 채팅방에서 `name`을 null로 보내면 서버에서 자동 생성하지 않으므로 클라이언트가 직접 지정하세요.

                    **프로젝트 연결**
                    - `projectId`는 선택 값입니다. 특정 프로젝트에 속한 채팅방을 만들 때 지정합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 생성(또는 기존 1:1 방 반환) 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "요청 유효성 오류 — `memberUserIds`가 비어 있거나, 존재하지 않는 사용자 ID 포함",
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
            description = "인증 실패 — Authorization 헤더 없거나 JWT가 만료됨",
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
            responseCode = "404",
            description = "`projectId`에 해당하는 프로젝트를 찾을 수 없거나, `memberUserIds`에 존재하지 않는 사용자 포함",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "NOT_FOUND", "message": "프로젝트를 찾을 수 없습니다" }
                                    }
                                    """
                    )
            )
    )
    ApiResponse<ChatRoomResponse> createRoom(@Valid ChatRoomCreateRequest request);

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = """
                    현재 로그인한 사용자가 참여 중인 채팅방 목록을 페이지네이션으로 반환합니다.
                    나간 채팅방(`leaveRoom` 호출 후)은 목록에 포함되지 않습니다.

                    **페이지네이션 파라미터 (Spring Pageable)**
                    | 파라미터 | 기본값 | 설명 |
                    |---|---|---|
                    | `page` | 0 | 0-indexed 페이지 번호 |
                    | `size` | 20 | 페이지당 항목 수 |
                    | `sort` | — | 정렬 기준 (예: `lastMessageAt,desc`) |

                    예시 요청: `GET /api/chat/rooms?page=0&size=20&sort=lastMessageAt,desc`
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공")
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
    ApiResponse<Page<ChatRoomResponse>> getMyRooms(Pageable pageable);

    @Operation(
            summary = "채팅방 단건 조회",
            description = """
                    `roomId`에 해당하는 채팅방의 상세 정보를 반환합니다.
                    현재 사용자가 해당 방의 활성 멤버(나가지 않은 상태)여야 조회할 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 조회 성공")
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
            description = "채팅방 멤버가 아님(나간 상태 포함)",
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "채팅방을 찾을 수 없음",
            content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "success": false,
                                      "data": null,
                                      "error": { "code": "NOT_FOUND", "message": "채팅방을 찾을 수 없습니다" }
                                    }
                                    """
                    )
            )
    )
    ApiResponse<ChatRoomResponse> getRoom(@Parameter(description = "조회할 채팅방 UUID") UUID roomId);

    @Operation(
            summary = "채팅방 나가기",
            description = """
                    현재 사용자가 채팅방에서 나갑니다.

                    - 나간 시각(`leftAt`)이 기록되며, 이후 해당 방의 메시지 조회나 전송이 불가합니다.
                    - OWNER가 나가더라도 다른 멤버에게 자동으로 권한이 이전되지 않습니다 — 권한 이전이 필요하면 별도 API를 사용하세요.
                    - 이미 나간 방에 대해 다시 요청하면 403을 반환합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 나가기 성공 (data: null)")
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
            description = "채팅방 멤버가 아니거나 이미 나간 상태",
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
    ApiResponse<Void> leaveRoom(@Parameter(description = "나갈 채팅방 UUID") UUID roomId);
}
