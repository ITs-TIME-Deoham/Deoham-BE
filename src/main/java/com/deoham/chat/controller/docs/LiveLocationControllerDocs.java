package com.deoham.chat.controller.docs;

import com.deoham.chat.dto.LiveLocationEvent;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;

@Tag(
        name = "채팅 실시간 위치 공유",
        description = """
                채팅방 참여자(카드 요청자 + 수락된 지원자) 간 위치를 실시간으로 주고받는 기능.
                채팅 화면 진입 시 자동으로 공유가 시작되고, 화면을 벗어나면 종료됩니다.
                위치는 DB에 저장하지 않고 짧은 TTL(30초)로만 캐시되는 휘발성 데이터입니다.

                **실시간 프로토콜 (WebSocket/STOMP)** — 아래는 REST가 아니라 STOMP 프레임입니다.
                - 연결: `ws://{host}/ws`, CONNECT 프레임에 `Authorization: Bearer {token}`
                - 구독(수신 시작 = 공유 시작): `SUBSCRIBE /sub/chat/rooms/{roomId}/live-location`
                - 위치 갱신 발행: `SEND /pub/chat/rooms/{roomId}/live-location`
                  body: `LiveLocationRequest` = `{ "latitude": 37.5665, "longitude": 126.9780, "accuracy": 5.0 }`
                - 명시적 종료 발행: `SEND /pub/chat/rooms/{roomId}/live-location/stop` (body 없음)

                **수신 이벤트** (`/sub/chat/rooms/{roomId}/live-location` 로 push되는 `LiveLocationEvent`)
                - `type=UPDATE`: 상대(또는 본인)의 위치 갱신. `senderId`로 누구 위치인지 구분.
                - `type=STOP`: 해당 `senderId`가 공유를 종료함(명시적 stop, 구독 해제, 연결 끊김 모두 포함).
                  좌표 필드는 null이며, 지도에서 해당 사용자의 마커를 제거하는 신호입니다.

                **유량 제어**: 서버는 참여자별 최소 1초 간격으로만 브로드캐스트하며, 그보다 촘촘한 갱신은 드롭합니다.

                아래 REST 엔드포인트는 화면 진입 시(늦은 입장 포함) 상대의 마지막 위치를 즉시 표시하기 위한 스냅샷 조회용입니다.
                """
)
public interface LiveLocationControllerDocs {

    @Operation(
            summary = "실시간 위치 스냅샷 조회",
            description = """
                    채팅방 참여자들의 마지막 위치(TTL 30초 이내에 캐시된 것)를 반환합니다.

                    채팅 화면 진입 직후 호출해, 상대가 이미 공유 중이라면 마지막 위치를 즉시 지도에 표시하는 데 사용합니다.
                    이후의 실시간 갱신은 `/sub/chat/rooms/{roomId}/live-location` 구독으로 받습니다.

                    - 반환 배열에는 본인과 상대의 위치가 모두 포함될 수 있으며, `senderId`로 구분합니다.
                    - 아직 아무도 공유하지 않았거나 캐시가 만료된 경우 빈 배열을 반환합니다.
                    - 모든 이벤트의 `type`은 `UPDATE`입니다(스냅샷에는 STOP이 포함되지 않음).
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "스냅샷 조회 성공. data는 참여자들의 마지막 위치(LiveLocationEvent) 배열")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 참여자가 아님",
            content = @Content(mediaType = "application/json",
                    examples = @ExampleObject(value = """
                            {"success":false,"data":null,"error":{"code":"FORBIDDEN","message":"채팅방 참여자가 아닙니다"}}
                            """)))
    ApiResponse<List<LiveLocationEvent>> getSnapshot(
            @Parameter(description = "채팅방 UUID") UUID roomId);
}
