package com.deoham.card.service;

import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;

import java.util.Optional;
import java.util.UUID;

public interface CardReadService {

    Optional<CardDetailResponse> getMyActiveCard(UUID userId);

    PaginatedCardListResponse getNearbyCards(double lat, double lng, String cursor);

    CardDetailResponse getCard(UUID cardId);
}
