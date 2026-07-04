package com.deoham.card.controller;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.MyActiveCardResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
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
import org.springframework.http.HttpStatus;
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
            description = """
                    도움 요청 카드를 생성합니다.
                    로그인 직후 GET /api/cards/my/active로 진행 중인 카드가 없는 것을 확인한 뒤 호출하는 API입니다.
                    생성된 카드는 OPEN 상태가 되며 주변 사용자에게 노출됩니다.
                    기본 만료 시간은 생성 시점 기준 2시간이고, retryCount는 0으로 시작합니다.
                    """
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
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(cardWriteService.createCard(request, userId)));
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "주변 카드 목록 조회",
            description = """
                    현재 위치 기준 반경 100m 내 OPEN 상태 카드를 거리순으로 반환합니다.
                    도움을 제공하려는 사용자가 주변의 도움 요청 카드를 탐색할 때 호출합니다.

                    [페이지네이션]
                    - 한 번의 요청으로 최대 20개의 카드를 반환합니다.
                    - cursor 기반 페이지네이션을 사용하여 중복/누락 없이 정확한 조회를 보장합니다.
                    - 서버는 내부적으로 21개를 조회하여 다음 페이지 존재 여부를 판단합니다.
                    - 21번째 카드가 있으면 nextCursor를 반환하고, 없으면 nextCursor는 null입니다.

                    [첫 요청]
                    - cursor 파라미터를 생략하면 가장 가까운 카드부터 반환됩니다.

                    [다음 페이지]
                    - 응답의 nextCursor를 다음 요청의 cursor 파라미터로 전달합니다.
                    - nextCursor는 가까운 순서로 20개씩 이어서 조회하기 위한 값입니다.
                    - nextCursor는 현재 페이지의 마지막 카드(20번째 카드)의 거리와 ID를 조합하여 Base64 인코딩한 값입니다.
                    - 예: "50.5|e5f6g7h8" → "NTAuNXxlNWY2ZzdoOA=="
                    - nextCursor 기준 이후의 카드들을 조회하므로 새로운 카드 생성 중에도 중복/누락이 발생하지 않습니다.

                    [마지막 페이지]
                    - nextCursor가 null이면 더 이상 조회할 카드가 없습니다.

                    목록에서 카드를 선택한 뒤 GET /api/cards/{cardId}로 상세 정보를 확인하고 신청 플로우로 이어집니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaginatedCardListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "위도/경도 파라미터 누락"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/cards/nearby")
    public ResponseEntity<ApiResponse<PaginatedCardListResponse>> getNearbyCards(
            @Parameter(description = "위도", required = true, example = "37.5326")
            @RequestParam @NotNull Double latitude,

            @Parameter(description = "경도", required = true, example = "126.9903")
            @RequestParam @NotNull Double longitude,

            @Parameter(
                    description = """
                            페이지 커서 (선택사항)
                            - 생략하면 첫 페이지 조회 (가장 가까운 카드부터)
                            - 다음 페이지: 이전 응답의 nextCursor 값을 사용
                            - nextCursor가 null이면 더 이상 조회할 카드가 없음
                            - 형식: 거리|카드ID를 Base64로 인코딩한 값
                            """,
                    example = "NTAuNXxhMWIyYzNkNGU1ZjY=")
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cardReadService.getNearbyCards(latitude, longitude, cursor)));
    }

    @Tag(name = "Card")
    @Operation(
            summary = "내 활성 카드 조회",
            description = """
                    로그인 직후 가장 먼저 호출해 현재 사용자가 생성한 진행 중 카드가 있는지 확인합니다.
                    현재 로그인한 사용자의 OPEN 또는 MATCHED 상태 카드를 반환합니다.
                    OPEN 카드는 아직 매칭되지 않은 도움 요청이고, MATCHED 카드는 신청 접수 후 진행 중인 카드입니다.
                    활성 카드가 없으면 card가 null이며, 이 경우 사용자는 새 카드를 생성할 수 있습니다.
                    hasCreatedCard 값으로 사용자가 이전에 카드를 생성했던 이력을 확인할 수 있습니다 (온보딩 용).
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = MyActiveCardResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/cards/my/active")
    public ResponseEntity<ApiResponse<MyActiveCardResponse>> getMyActiveCard() {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.ok(ApiResponse.ok(cardReadService.getMyActiveCard(userId)));
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 상세 조회",
            description = """
                    카드 ID로 상세 정보를 반환합니다.
                    주변 카드 목록에서 특정 카드를 선택했을 때 상세 내용을 확인하는 API입니다.
                    도움을 제공하려는 사용자는 POST /api/cards/{cardId}/applies로 신청할 수 있습니다.
                    추후 알림 기능을 위해 사용될 API입니다.
                    """
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
        return ResponseEntity.ok(ApiResponse.ok(cardReadService.getCard(cardId)));
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 취소",
            description = """
                    OPEN 상태의 카드를 CANCELLED로 변경합니다.
                    카드 작성자만 호출할 수 있으며, 아직 신청을 수락해 MATCHED가 되기 전의 요청을 취소할 때 사용합니다.
                    취소된 카드는 활성 카드 조회 대상에서 제외됩니다.
                    """
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
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.cancelCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 완료",
            description = """
                    MATCHED 상태의 카드를 COMPLETED로 변경합니다.
                    신청 매칭 후 실제 도움이 완료되었을 때 카드 작성자가 호출합니다.
                    완료 시 매칭된 신청자의 help_count가 증가하고, 카드는 활성 카드 조회 대상에서 제외됩니다.
                    """
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
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.completeCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Card")
    @Operation(
            summary = "카드 재요청",
            description = """
                    OPEN 상태의 카드를 다시 주변 사용자에게 노출하기 위해 재요청합니다.
                    카드 작성자만 호출할 수 있으며, 최대 3회까지 가능합니다.
                    재요청 시 retryCount가 1 증가하고 만료 시간이 현재 시점 기준 2시간 뒤로 초기화됩니다.
                    MATCHED 이후에는 재요청할 수 없습니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "204", description = "재요청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "카드 작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "카드를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "재요청 불가 (OPEN 상태가 아니거나 재요청 횟수 3회 초과)")
    })
    @PatchMapping("/cards/{cardId}/retry")
    public ResponseEntity<ApiResponse<Void>> retryCard(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.retryCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------
    // CardApply (신청) endpoints
    // ----------------------------------------------------------------

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청 제출",
            description = """
                    도움을 제공하려는 사용자가 OPEN 상태의 카드에 신청합니다.
                    일반적인 흐름은 주변 카드 목록 조회 → 신청 제출입니다.
                    카드 작성자는 본인 카드에 신청할 수 없으며, 카드 1개당 1회만 신청 가능합니다.
                    신청은 PENDING 상태로 생성되고, 카드 작성자의 수락 또는 거절을 기다리는 절차 없이 바로 매칭됩니다.
                    """
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
    public ResponseEntity<ApiResponse<CardApplySummaryResponse>> submitApply(
            @Parameter(description = "카드 ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(cardWriteService.submitApply(cardId, userId)));
    }

    @Tag(name = "CardApply")
    @Operation(
            summary = "신청 목록 조회",
            description = """
                    카드에 달린 신청 목록을 반환합니다.
                    카드 작성자만 호출할 수 있으며, 매칭된 신청자 정보를 확인할 때 사용합니다.
                    """
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
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.ok(ApiResponse.ok(cardReadService.getApplies(cardId, userId)));
    }

}
