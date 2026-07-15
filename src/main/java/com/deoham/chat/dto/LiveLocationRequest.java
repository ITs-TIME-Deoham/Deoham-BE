package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 채팅방 참여자가 실시간으로 전송하는 위치 갱신 요청.
 * STOMP 목적지 {@code /pub/chat/rooms/{roomId}/live-location} 로 수신한다.
 */
@Schema(description = "실시간 위치 갱신 요청 (STOMP SEND /pub/chat/rooms/{roomId}/live-location)")
public record LiveLocationRequest(

        @Schema(description = "위도", example = "37.5665", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Double latitude,

        @Schema(description = "경도", example = "126.9780", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull Double longitude,

        @Schema(description = "위치 정확도(미터). 선택값", example = "5.0")
        Double accuracy
) {
}
