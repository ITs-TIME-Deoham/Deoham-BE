package com.deoham.card.controller;

import com.deoham.card.controller.docs.CardApplyControllerDocs;
import com.deoham.card.controller.docs.CardControllerDocs;
import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.MyActiveCardResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.response.ApiResponse;
import com.deoham.global.security.AuthenticationUtils;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
public class CardController implements CardControllerDocs, CardApplyControllerDocs {

    private final CardReadService cardReadService;
    private final CardWriteService cardWriteService;

    @Override
    @PostMapping("/cards")
    public ResponseEntity<CardDetailResponse> createCard(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Card create request", required = true,
                    content = @Content(schema = @Schema(implementation = CreateCardRequest.class)))
            @Valid @RequestBody CreateCardRequest request
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cardWriteService.createCard(request, userId));
    }

    @Override
    @GetMapping("/cards/nearby")
    public ResponseEntity<PaginatedCardListResponse> getNearbyCards(
            @RequestParam @NotNull Double latitude,
            @RequestParam @NotNull Double longitude,
            @Parameter(description = "Optional pagination cursor 선택 항목", example = "NTAuNXxhMWIyYzNkNGU1ZjY=")
            @RequestParam(required = false) String cursor
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        return ResponseEntity.ok(cardReadService.getNearbyCards(latitude, longitude, cursor, userId));
    }

    @Override
    @GetMapping("/cards/my/active")
    public ResponseEntity<MyActiveCardResponse> getMyActiveCard() {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        return ResponseEntity.ok(cardReadService.getMyActiveCard(userId));
    }

    @Override
    @GetMapping("/cards/{cardId}")
    public ResponseEntity<CardDetailResponse> getCard(@PathVariable UUID cardId) {
        return ResponseEntity.ok(cardReadService.getCard(cardId));
    }

    @Override
    @PatchMapping("/cards/{cardId}/cancel")
    public ResponseEntity<Void> cancelCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        cardWriteService.cancelCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/cards/{cardId}/complete")
    public ResponseEntity<Void> completeCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        cardWriteService.completeCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PatchMapping("/cards/{cardId}/retry")
    public ResponseEntity<Void> retryCard(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        cardWriteService.retryCard(cardId, userId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @PostMapping("/cards/{cardId}/applies")
    public ResponseEntity<CardApplySummaryResponse> submitApply(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        return ResponseEntity.status(HttpStatus.CREATED).body(cardWriteService.submitApply(cardId, userId));
    }

    @Override
    @GetMapping("/cards/{cardId}/applies")
    public ResponseEntity<List<CardApplySummaryResponse>> getApplies(
            @Parameter(description = "Card ID", required = true)
            @PathVariable UUID cardId
    ) {
        UUID userId = AuthenticationUtils.currentPrincipal()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "인증 정보를 찾을 수 없습니다."))
                .userId();
        return ResponseEntity.ok(cardReadService.getApplies(cardId, userId));
    }
}
