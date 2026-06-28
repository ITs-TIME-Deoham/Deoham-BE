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

@Tag(name = "채팅방", description = "카드 1:1 채팅방 생성·조회·종료 API")
public interface ChatRoomControllerDocs {

    @Operation(
            summary = "채팅방 생성 또는 조회",
            description = """
                    카드 UUID로 1:1 채팅방을 가져옵니다. 채팅방이 없으면 새로 생성합니다(멱등).

                    요청자는 해당 카드의 요청자(requester) 또는 수락된 신청자(accepted applicant)여야 합니다.
                    카드 1개에 채팅방은 정확히 1개만 존재합니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 조회 또는 생성 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            description = "해당 카드의 참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """))
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "카드를 찾을 수 없음",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"NOT_FOUND","message":"카드를 찾을 수 없습니다"}}
                            """))
    )
    ApiResponse<ChatRoomResponse> getOrCreateRoom(@Valid ChatRoomCreateRequest request);

    @Operation(
            summary = "내 채팅방 목록 조회",
            description = "현재 사용자가 참여 중인 채팅방 목록을 최신순으로 반환합니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공")
    ApiResponse<Page<ChatRoomResponse>> getMyRooms(Pageable pageable);

    @Operation(
            summary = "채팅방 단건 조회",
            description = "roomId에 해당하는 채팅방 정보를 반환합니다. 참여자만 조회할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<ChatRoomResponse> getRoom(@Parameter(description = "채팅방 UUID") UUID roomId);

    @Operation(
            summary = "채팅방 종료",
            description = """
                    채팅방을 CLOSED 상태로 변경합니다. 종료 후에는 메시지 전송이 불가합니다.
                    참여자(요청자 또는 수락된 신청자) 누구나 종료할 수 있습니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공 (data: null)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<Void> closeRoom(@Parameter(description = "종료할 채팅방 UUID") UUID roomId);
}
