package com.deoham.card.dto.response;

import com.deoham.card.entity.CardApplyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Card apply summary response")
public record CardApplySummaryResponse(

        @Schema(description = "Apply ID")
        UUID id,

        @Schema(description = "Applicant ID")
        UUID applicantId,

        @Schema(description = "Applicant nickname")
        String applicantNickname,

        @Schema(description = "Applicant profile image URL")
        String applicantProfileImageUrl,

        @Schema(description = "Apply status")
        CardApplyStatus status,

        @Schema(description = "Applied time")
        Instant appliedAt
) {
}
