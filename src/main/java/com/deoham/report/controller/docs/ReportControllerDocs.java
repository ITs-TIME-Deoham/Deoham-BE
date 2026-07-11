package com.deoham.report.controller.docs;

import com.deoham.report.dto.ReportCreateRequest;
import com.deoham.report.dto.ReportResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "신고", description = "사용자 신고 API")
public interface ReportControllerDocs {

    @Operation(
            summary = "사용자 신고",
            description = """
                    채팅 중 상대방 사용자를 신고합니다.

                    **신고 프로세스:**
                    1. 신고 기록이 생성됩니다
                    2. 신고한 사용자와 신고당한 사용자가 즉시 상호 차단됩니다
                    3. 진행 중인 채팅방이 종료됩니다
                    4. 신고당한 사용자의 누적 신고가 5건 이상이면 계정이 자동으로 정지됩니다

                    **필수 입력:** description은 신고 상세 내용을 나타내므로 필수입니다.

                    **중복 신고 불가:** 같은 사용자를 여러 번 신고할 수 없습니다.
                    이미 신고한 사용자를 다시 신고하면 409 Conflict 에러가 반환됩니다.
                    """
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201",
            description = "신고 접수 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 요청 (필수 필드 누락, 유효성 검사 실패)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401",
            description = "인증 실패 (로그인 필요)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "신고당할 사용자를 찾을 수 없음",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "이미 신고한 사용자 (중복 신고)",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ApiResponse.class)
            )
    )
    ResponseEntity<ReportResponse> createReport(@Valid ReportCreateRequest request);
}
