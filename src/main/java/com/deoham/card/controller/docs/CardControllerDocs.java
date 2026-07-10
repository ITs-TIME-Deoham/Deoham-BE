package com.deoham.card.controller.docs;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.MyActiveCardResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@Tag(name = "Card")
public interface CardControllerDocs {

    @Operation(summary = "Create card", description = "Creates a help request card.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "201", description = "Created",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CardDetailResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<CardDetailResponse> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Card create request", required = true,
                    content = @Content(schema = @Schema(implementation = CreateCardRequest.class)))
            @Valid CreateCardRequest request);

    @Operation(summary = "Get nearby cards", description = "Returns nearby OPEN cards sorted by distance.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = PaginatedCardListResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400", description = "Invalid latitude or longitude",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<PaginatedCardListResponse> getNearbyCards(
            @Parameter(description = "Latitude", required = true, example = "37.5326") Double latitude,
            @Parameter(description = "Longitude", required = true, example = "126.9903") Double longitude,
            @Parameter(description = "Optional pagination cursor", example = "NTAuNXxhMWIyYzNkNGU1ZjY=") String cursor);

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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "조회 성공",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = MyActiveCardResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<MyActiveCardResponse> getMyActiveCard();

    @Operation(summary = "Get card detail", description = "Returns detail for a card.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200", description = "OK",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CardDetailResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Card not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<CardDetailResponse> getCard(@Parameter(description = "Card ID", required = true) UUID cardId);

    @Operation(summary = "Cancel card", description = "Cancels an OPEN card.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Card not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Card status conflict",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<Void> cancelCard(@Parameter(description = "Card ID", required = true) UUID cardId);

    @Operation(summary = "Complete card", description = "Completes a MATCHED card.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Card not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Card status conflict",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<Void> completeCard(@Parameter(description = "Card ID", required = true) UUID cardId);

    @Operation(summary = "Retry card", description = "Refreshes an OPEN card request.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "No Content")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "401", description = "Unauthorized",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403", description = "Forbidden",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404", description = "Card not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409", description = "Card status conflict",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ApiResponse.class)))
    ResponseEntity<Void> retryCard(@Parameter(description = "Card ID", required = true) UUID cardId);
}
