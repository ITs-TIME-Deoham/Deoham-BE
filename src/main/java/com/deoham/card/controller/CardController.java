package com.deoham.card.controller;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.CardSummaryResponse;
import com.deoham.card.dto.response.MyCardApplySummaryResponse;
import com.deoham.card.dto.response.MyCardSummaryResponse;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CardController {

    private final CardReadService cardReadService;
    private final CardWriteService cardWriteService;

    // ----------------------------------------------------------------
    // Card (게시물) endpoints
    // ----------------------------------------------------------------

    @Tag(name = "Card")
    @Operation(
            summary = "카드 생성",
            description = "도움 요청 카드를 생성합니다. 생성된 카드는 OPEN 상태로 주변 사용자에게 노출됩니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "생성 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CardDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "잘못된 요청 (필수 필드 누락 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/cards")
    public ResponseEntity<ApiResponse<CardDetailResponse>> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "카드 생성 요청 본문", required = true,
                    content = @Content(schema = @Schema(implementation = CreateCardRequest.class)))
            @Valid @RequestBody CreateCardRequest request
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "Card")
    @Operation(
            summary = "주변 카드 목록 조회",
            description = "현재 위치 기준 반경 내 OPEN 상태 카드를 거리순으로 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CardSummaryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "위도/경도 파라미터 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/cards/nearby")
    public ResponseEntity<ApiResponse<List<CardSummaryResponse>>> getNearbyCards(
            @Parameter(description = "위도", required = true, example = "37.5326")
            @RequestParam @NotNull Double latitude,

            @Parameter(description = "경도", required = true, example = "126.9903")
            @RequestParam @NotNull Double longitude,

            @Parameter(description = "검색 반경 (미터, 기본값 1000)", example = "1000")
            @RequestParam(defaultValue = "1000") Double radiusMeters
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "Card")
    @Operation(
            summary = "내 카드 목록 조회",
            description = "현재 로그인한 사용자가 작성한 카드 목록을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MyCardSummaryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/cards/my")
    public ResponseEntity<ApiResponse<List<MyCardSummaryResponse>>> getMyCards() {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 상세 조회",
            description = "카드 ID로 상세 정보를 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CardDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음")
    })
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> getCard(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 취소",
            description = "OPEN 상태의 카드를 CANCELLED로 변경합니다. 카드 작성자만 호출할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "OPEN 상태가 아님 (이미 매칭됐거나 완료됨)")
    })
    @PatchMapping("/cards/{cardId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelCard(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 완료",
            description = "MATCHED 상태의 카드를 COMPLETED로 변경합니다. 카드 작성자만 호출할 수 있으며, 도움을 제공한 사용자의 help_count가 증가합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "MATCHED 상태가 아님")
    })
    @PatchMapping("/cards/{cardId}/complete")
    public ResponseEntity<ApiResponse<Void>> completeCard(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // ----------------------------------------------------------------
    // CardApply (신청서) endpoints
    // ----------------------------------------------------------------

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청서 제출",
            description = "OPEN 상태의 카드에 신청서를 제출합니다. 카드 작성자는 본인 카드에 신청할 수 없으며, 카드 1개당 1회만 신청 가능합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "신청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 카드에는 신청 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "이미 신청했거나 카드가 OPEN 상태가 아님")
    })
    @PostMapping("/cards/{cardId}/applies")
    public ResponseEntity<ApiResponse<Void>> submitApply(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청서 취소",
            description = "본인이 제출한 PENDING 상태의 신청서를 취소합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 신청서가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "신청서를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "PENDING 상태가 아님 (이미 수락 또는 거절됨)")
    })
    @DeleteMapping("/cards/{cardId}/applies")
    public ResponseEntity<ApiResponse<Void>> cancelApply(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청서 목록 조회",
            description = "카드에 달린 신청서 목록을 반환합니다. 카드 작성자만 호출할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CardApplySummaryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음")
    })
    @GetMapping("/cards/{cardId}/applies")
    public ResponseEntity<ApiResponse<List<CardApplySummaryResponse>>> getApplies(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청서 수락",
            description = """
                    신청서를 수락합니다. 수락 시 아래 작업이 하나의 트랜잭션으로 처리됩니다.
                    1. 해당 신청서 상태 → ACCEPTED
                    2. 카드 상태 → MATCHED
                    3. 나머지 PENDING 신청서 → REJECTED
                    4. 채팅방 생성
                    카드 작성자만 호출할 수 있습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "수락 성공 (채팅방 생성됨)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "신청서를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "PENDING 상태가 아니거나 카드가 OPEN 상태가 아님")
    })
    @PatchMapping("/cards/{cardId}/applies/{applyId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptApply(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId,

            @Parameter(description = "신청서 ID", required = true)
            @PathVariable UUID applyId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청서 거절",
            description = "신청서를 거절합니다. 카드 작성자만 호출할 수 있습니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "거절 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "신청서를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "PENDING 상태가 아님")
    })
    @PatchMapping("/cards/{cardId}/applies/{applyId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApply(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId,

            @Parameter(description = "신청서 ID", required = true)
            @PathVariable UUID applyId
    ) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "내 신청서 목록 조회",
            description = "현재 로그인한 사용자가 제출한 신청서 목록을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = MyCardApplySummaryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/applies/my")
    public ResponseEntity<ApiResponse<List<MyCardApplySummaryResponse>>> getMyApplies() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
