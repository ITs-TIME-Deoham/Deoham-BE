package com.deoham.report.dto;

import com.deoham.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "채팅방 기준 신고 생성 요청 (상대방 UUID는 채팅방 참여자 정보에서 서버가 직접 조회)")
public record ChatRoomReportCreateRequest(

        @Schema(
                description = "신고 사유",
                example = "HARASSMENT",
                allowableValues = {"HARASSMENT", "OBSCENE_CONTENT", "PRIVACY_VIOLATION"}
        )
        @NotNull(message = "신고 사유는 필수입니다")
        ReportReason reason,

        @Schema(
                description = "신고 상세 설명 (1~500자, 필수)",
                example = "욕설과 협박 메시지를 보냈습니다"
        )
        @NotBlank(message = "신고 설명은 필수입니다")
        @Size(min = 1, max = 500, message = "신고 설명은 1자 이상 500자 이하여야 합니다")
        String description
) {
}