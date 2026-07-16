package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * 실시간 위치 브로드캐스트/스냅샷 페이로드.
 * STOMP 목적지 {@code /sub/chat/rooms/{roomId}/live-location} 로 전송하며,
 * {@link Type#STOP} 인 경우 좌표 필드는 null 이다(상대 지도에서 마커 제거 신호).
 */
@Schema(description = "실시간 위치 이벤트 (STOMP /sub/chat/rooms/{roomId}/live-location 및 스냅샷 REST 응답)")
public record LiveLocationEvent(

        @Schema(description = "이벤트 종류. UPDATE=위치 갱신, STOP=공유 종료(좌표 null, 마커 제거)", example = "UPDATE")
        Type type,

        @Schema(description = "위치 주체 사용자 UUID (누구의 위치인지 구분)")
        UUID senderId,

        @Schema(description = "위도. STOP 이벤트에서는 null", example = "37.5665", nullable = true)
        Double latitude,

        @Schema(description = "경도. STOP 이벤트에서는 null", example = "126.9780", nullable = true)
        Double longitude,

        @Schema(description = "위치 정확도(미터). STOP 또는 미측정 시 null", example = "5.0", nullable = true)
        Double accuracy,

        @Schema(description = "이벤트 발생 시각 (ISO-8601)", example = "2026-07-14T07:49:42.994Z")
        Instant sentAt
) {

    @Schema(description = "실시간 위치 이벤트 종류")
    public enum Type {
        UPDATE,
        STOP
    }

    public static LiveLocationEvent update(UUID senderId, LiveLocationRequest request, Instant sentAt) {
        return new LiveLocationEvent(
                Type.UPDATE,
                senderId,
                request.latitude(),
                request.longitude(),
                request.accuracy(),
                sentAt);
    }

    public static LiveLocationEvent stop(UUID senderId, Instant sentAt) {
        return new LiveLocationEvent(Type.STOP, senderId, null, null, null, sentAt);
    }
}
