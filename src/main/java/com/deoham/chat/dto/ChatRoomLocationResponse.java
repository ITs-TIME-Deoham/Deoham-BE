package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "채팅방 카드 위치 정보")
public record ChatRoomLocationResponse(

        @Schema(description = "위도", example = "37.5326")
        Double latitude,

        @Schema(description = "경도", example = "126.9903")
        Double longitude,

        @Schema(description = "도시명", example = "서울특별시")
        String city
) {
}
