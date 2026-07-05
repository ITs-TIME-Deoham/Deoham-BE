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

    @Tag(name = "Card")
    @Operation(summary = "Create card", description = "Creates a help request card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CardDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid request",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/cards")
    public ResponseEntity<CardDetailResponse> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Card create request", required = true,
                    content = @Content(schema = @Schema(implementation = CreateCardRequest.class)))
            @Valid @RequestBody CreateCardRequest request
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cardWriteService.createCard(request, userId));
    }

    @Tag(name = "Card")
    @Operation(summary = "Get nearby cards", description = "Returns nearby OPEN cards sorted by distance.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PaginatedCardListResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid latitude or longitude",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/cards/nearby")
    public ResponseEntity<PaginatedCardListResponse> getNearbyCards(
            @Parameter(description = "Latitude", required = true, example = "37.5326")
            @RequestParam @NotNull Double latitude,
            @Parameter(description = "Longitude", required = true, example = "126.9903")
            @RequestParam @NotNull Double longitude,
            @Parameter(description = "Optional pagination cursor", example = "NTAuNXxhMWIyYzNkNGU1ZjY=")
            @RequestParam(required = false) String cursor
    ) {
        return ResponseEntity.ok(cardReadService.getNearbyCards(latitude, longitude, cursor));
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
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/cards/my/active")
    public ResponseEntity<MyActiveCardResponse> getMyActiveCard() {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.ok(cardReadService.getMyActiveCard(userId));
    }

    @Tag(name = "Card")
    @Operation(summary = "Get card detail", description = "Returns detail for a card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CardDetailResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<CardDetailResponse> getCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        return ResponseEntity.ok(cardReadService.getCard(cardId));
    }

    @Tag(name = "Card")
    @Operation(summary = "Cancel card", description = "Cancels an OPEN card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Card status conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/cards/{cardId}/cancel")
    public ResponseEntity<Void> cancelCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.cancelCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Card")
    @Operation(summary = "Complete card", description = "Completes a MATCHED card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Card status conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/cards/{cardId}/complete")
    public ResponseEntity<Void> completeCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.completeCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "Card")
    @Operation(summary = "Retry card", description = "Refreshes an OPEN card request.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Card status conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PatchMapping("/cards/{cardId}/retry")
    public ResponseEntity<Void> retryCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        cardWriteService.retryCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Tag(name = "CardApply")
    @Operation(summary = "Submit card apply", description = "Applies to an OPEN card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "Created",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CardApplySummaryResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Card apply conflict",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @PostMapping("/cards/{cardId}/applies")
    public ResponseEntity<CardApplySummaryResponse> submitApply(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cardWriteService.submitApply(cardId, userId));
    }

    @Tag(name = "CardApply")
    @Operation(summary = "Get card applies", description = "Returns applies submitted to a card.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "OK",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CardApplySummaryResponse.class)))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Unauthorized",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Forbidden",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Card not found",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ApiResponse.class)))
    })
    @GetMapping("/cards/{cardId}/applies")
    public ResponseEntity<List<CardApplySummaryResponse>> getApplies(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal().orElseThrow().userId();
        return ResponseEntity.ok(cardReadService.getApplies(cardId, userId));
    }
}
