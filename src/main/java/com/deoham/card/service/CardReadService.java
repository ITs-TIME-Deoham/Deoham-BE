package com.deoham.card.service;

import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.CardSummaryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardReadService {

    Optional<CardDetailResponse> getMyActiveCard(UUID userId);

    List<CardSummaryResponse> getNearbyCards(double lat, double lng, double radiusMeters);

    CardDetailResponse getCard(UUID cardId);
}
