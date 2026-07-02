package com.deoham.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "LOCATION 타입 메시지의 위치 정보")
public record LocationPayload(

        @NotNull
        @Schema(description = "위도", example = "37.5326")
        Double latitude,

        @NotNull
        @Schema(description = "경도", example = "126.9903")
        Double longitude,

        @Schema(description = "장소명 (선택)", example = "한강공원 뚝섬지구")
        String placeName
) {
}
