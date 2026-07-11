package com.deoham.report.dto;

import com.deoham.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Schema(description = "신고 생성 요청")
public record ReportCreateRequest(

        @Schema(
                description = "신고 대상자(신고당한 사용자)의 UUID",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        )
        @NotNull(message = "신고당한 사용자 ID는 필수입니다")
        UUID reportedUserId,

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
