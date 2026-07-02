package com.deoham.card.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.exception.GlobalExceptionHandler;
import com.deoham.global.security.jwt.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(CardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class CardControllerTest {

    private static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID CARD_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID APPLY_ID = UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final Instant NOW = Instant.parse("2026-07-02T00:00:00Z");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean CardReadService cardReadService;
    @MockBean CardWriteService cardWriteService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createCard_returnsCreatedCard() throws Exception {
        when(cardWriteService.createCard(any(CreateCardRequest.class), eq(USER_ID)))
                .thenReturn(cardDetail(CardStatus.OPEN, null));

        mockMvc.perform(post("/api/cards")
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateCardBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(CARD_ID.toString()))
                .andExpect(jsonPath("$.data.requesterId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.data.category").value("PHOTO"))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        verify(cardWriteService).createCard(any(CreateCardRequest.class), eq(USER_ID));
    }

    @Test
    void createCard_rejectsMissingRequiredField() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "description", "Missing category",
                                "latitude", 37.5326,
                                "longitude", 126.9903))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    @Test
    void createCard_returnsNotFoundWhenPrincipalUserDoesNotExist() throws Exception {
        when(cardWriteService.createCard(any(CreateCardRequest.class), eq(USER_ID)))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "User not found"));

        mockMvc.perform(post("/api/cards")
                        .with(auth(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validCreateCardBody())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void getNearbyCards_returnsPaginatedCards() throws Exception {
        when(cardReadService.getNearbyCards(37.5326, 126.9903, null))
                .thenReturn(new PaginatedCardListResponse(List.of(cardDetail(CardStatus.OPEN, 12.3)), null));

        mockMvc.perform(get("/api/cards/nearby")
                        .with(auth(USER_ID))
                        .param("latitude", "37.5326")
                        .param("longitude", "126.9903"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cards.length()").value(1))
                .andExpect(jsonPath("$.data.cards[0].id").value(CARD_ID.toString()))
                .andExpect(jsonPath("$.data.cards[0].distanceMeters").value(12.3));
    }

    @Test
    void getMyActiveCard_returnsActiveCard() throws Exception {
        when(cardReadService.getMyActiveCard(USER_ID))
                .thenReturn(Optional.of(cardDetail(CardStatus.MATCHED, null)));

        mockMvc.perform(get("/api/cards/my/active").with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(CARD_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("MATCHED"));
    }

    @Test
    void getMyActiveCard_returnsNullWhenNoActiveCard() throws Exception {
        when(cardReadService.getMyActiveCard(USER_ID)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/cards/my/active").with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void getCard_returnsCardDetail() throws Exception {
        when(cardReadService.getCard(CARD_ID)).thenReturn(cardDetail(CardStatus.OPEN, null));

        mockMvc.perform(get("/api/cards/{cardId}", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(CARD_ID.toString()));
    }

    @Test
    void getCard_returnsNotFoundForUnknownCard() throws Exception {
        when(cardReadService.getCard(CARD_ID))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Card not found"));

        mockMvc.perform(get("/api/cards/{cardId}", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void cancelCard_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/cards/{cardId}/cancel", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isNoContent());

        verify(cardWriteService).cancelCard(CARD_ID, USER_ID);
    }

    @Test
    void cancelCard_rejectsNonOwner() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "Only owner can cancel"))
                .when(cardWriteService).cancelCard(CARD_ID, OTHER_USER_ID);

        mockMvc.perform(patch("/api/cards/{cardId}/cancel", CARD_ID).with(auth(OTHER_USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void cancelCard_rejectsNonOpenCard() throws Exception {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "Only OPEN cards can be cancelled"))
                .when(cardWriteService).cancelCard(CARD_ID, USER_ID);

        mockMvc.perform(patch("/api/cards/{cardId}/cancel", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void completeCard_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/cards/{cardId}/complete", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isNoContent());

        verify(cardWriteService).completeCard(CARD_ID, USER_ID);
    }

    @Test
    void completeCard_rejectsNonMatchedCard() throws Exception {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "Only MATCHED cards can be completed"))
                .when(cardWriteService).completeCard(CARD_ID, USER_ID);

        mockMvc.perform(patch("/api/cards/{cardId}/complete", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void retryCard_returnsNoContent() throws Exception {
        mockMvc.perform(patch("/api/cards/{cardId}/retry", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isNoContent());

        verify(cardWriteService).retryCard(CARD_ID, USER_ID);
    }

    @Test
    void retryCard_rejectsMaxRetryCount() throws Exception {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "Retry count exceeded"))
                .when(cardWriteService).retryCard(CARD_ID, USER_ID);

        mockMvc.perform(patch("/api/cards/{cardId}/retry", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void submitApply_returnsCreatedApply() throws Exception {
        when(cardWriteService.submitApply(CARD_ID, OTHER_USER_ID)).thenReturn(applySummary());

        mockMvc.perform(post("/api/cards/{cardId}/applies", CARD_ID).with(auth(OTHER_USER_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(APPLY_ID.toString()))
                .andExpect(jsonPath("$.data.applicantId").value(OTHER_USER_ID.toString()))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void submitApply_rejectsRequesterApplyingToOwnCard() throws Exception {
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "Cannot apply to own card"))
                .when(cardWriteService).submitApply(CARD_ID, USER_ID);

        mockMvc.perform(post("/api/cards/{cardId}/applies", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void submitApply_rejectsDuplicateOrNonOpenCard() throws Exception {
        doThrow(new BusinessException(ErrorCode.CONFLICT, "Cannot apply"))
                .when(cardWriteService).submitApply(CARD_ID, OTHER_USER_ID);

        mockMvc.perform(post("/api/cards/{cardId}/applies", CARD_ID).with(auth(OTHER_USER_ID)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CONFLICT"));
    }

    @Test
    void getApplies_returnsAppliesForOwner() throws Exception {
        when(cardReadService.getApplies(CARD_ID, USER_ID)).thenReturn(List.of(applySummary()));

        mockMvc.perform(get("/api/cards/{cardId}/applies", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(APPLY_ID.toString()))
                .andExpect(jsonPath("$.data[0].applicantId").value(OTHER_USER_ID.toString()));
    }

    @Test
    void getApplies_rejectsNonOwner() throws Exception {
        when(cardReadService.getApplies(CARD_ID, OTHER_USER_ID))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "Only owner can view applies"));

        mockMvc.perform(get("/api/cards/{cardId}/applies", CARD_ID).with(auth(OTHER_USER_ID)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    void getApplies_returnsNotFoundForUnknownCard() throws Exception {
        when(cardReadService.getApplies(CARD_ID, USER_ID))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "Card not found"));

        mockMvc.perform(get("/api/cards/{cardId}/applies", CARD_ID).with(auth(USER_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private Map<String, Object> validCreateCardBody() {
        return Map.of(
                "category", "PHOTO",
                "description", "Need help",
                "latitude", 37.5326,
                "longitude", 126.9903,
                "preferredGender", "ANY",
                "preferredAgeMin", 20,
                "preferredAgeMax", 50);
    }

    private CardDetailResponse cardDetail(CardStatus status, Double distanceMeters) {
        return new CardDetailResponse(
                CARD_ID,
                USER_ID,
                "https://example.com/profile.png",
                CardCategory.PHOTO,
                "Need help",
                NOW.plusSeconds(7200),
                status,
                PreferredGender.ANY,
                20,
                50,
                0,
                NOW,
                NOW,
                distanceMeters);
    }

    private CardApplySummaryResponse applySummary() {
        return new CardApplySummaryResponse(
                APPLY_ID,
                OTHER_USER_ID,
                "applicant",
                null,
                CardApplyStatus.PENDING,
                NOW);
    }

    private RequestPostProcessor auth(UUID userId) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt(userId)));
            return request;
        };
    }

    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(userId.toString())
                .claim("email", userId + "@test.local")
                .claim("role", "USER")
                .claim("type", "access")
                .issuedAt(NOW.minusSeconds(60))
                .expiresAt(NOW.plusSeconds(3600))
                .build();
    }
}
