package com.deoham.card.controller;

import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.service.CardReadService;
import com.deoham.card.service.CardWriteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CardController.class)
@DisplayName("CardController - GET /api/cards/nearby 테스트")
class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CardReadService cardReadService;

    @MockBean
    private CardWriteService cardWriteService;

    @Test
    @DisplayName("근처 카드 조회 - JWT Mock 데이터로 테스트")
    void testGetNearbyCards_WithMockJwt() throws Exception {
        // Given
        UUID userId = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");

        UUID cardId = UUID.randomUUID();
        UUID requesterId = UUID.randomUUID();
        Instant now = Instant.now();

        CardDetailResponse card = new CardDetailResponse(
                cardId,
                requesterId,
                "테스트유저",
                "https://example.com/profile.jpg",
                CardCategory.MEAL,
                "밥 함께 먹을 사람 찾습니다",
                now.plusSeconds(1800),
                CardStatus.OPEN,
                PreferredGender.ANY,
                20,
                50,
                0,
                now,
                now,
                150.5
        );

        PaginatedCardListResponse response = new PaginatedCardListResponse(
                List.of(card),
                "dGVzdF9jdXJzb3I=",
                true
        );

        when(cardReadService.getNearbyCards(
                eq(37.5326),
                eq(126.9903),
                any(),
                eq(userId)
        )).thenReturn(response);

        // When & Then - Spring Security의 jwt() helper 사용
        mockMvc.perform(get("/api/cards/nearby")
                .param("latitude", "37.5326")
                .param("longitude", "126.9903")
                .with(jwt()
                        .jwt(jwtBuilder -> jwtBuilder
                                .subject(userId.toString())
                                .claim("email", "test@example.com")
                                .claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cards").isArray())
                .andExpect(jsonPath("$.cards.length()").value(1))
                .andExpect(jsonPath("$.cards[0].id").value(cardId.toString()))
                .andExpect(jsonPath("$.cards[0].requesterNickname").value("테스트유저"))
                .andExpect(jsonPath("$.cards[0].category").value("MEAL"))
                .andExpect(jsonPath("$.cards[0].status").value("OPEN"))
                .andExpect(jsonPath("$.cards[0].retryCount").value(0))
                .andExpect(jsonPath("$.cards[0].distanceMeters").value(150.5))
                .andExpect(jsonPath("$.nextCursor").value("dGVzdF9jdXJzb3I="))
                .andExpect(jsonPath("$.hasSeenCardViewOnboarding").value(true));
    }
}
