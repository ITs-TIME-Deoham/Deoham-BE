package com.deoham.report.dto;

import com.deoham.report.entity.Report;
import com.deoham.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "신고 응답")
public record ReportResponse(

        @Schema(
                description = "신고 ID",
                example = "f47ac10b-58cc-4372-a567-0e02b2c3d479"
        )
        UUID reportId,

        @Schema(
                description = "신고 대상자(신고당한 사용자)의 UUID",
                example = "a12bc34d-56ef-78gh-ijkl-901m234n5678"
        )
        UUID reportedUserId,

        @Schema(
                description = "신고 사유",
                example = "HARASSMENT"
        )
        ReportReason reason,

        @Schema(
                description = "신고 상세 설명",
                example = "욕설과 협박 메시지를 보냈습니다"
        )
        String description,

        @Schema(
                description = "신고 생성 시간",
                example = "2026-07-11T10:30:00Z"
        )
        Instant createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getReportedUser().getId(),
                report.getReason(),
                report.getDescription(),
                report.getCreatedAt()
        );
    }
}
