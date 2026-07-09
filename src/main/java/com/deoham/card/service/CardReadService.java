package com.deoham.card.service;

import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.MyActiveCardResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;

import java.util.List;
import java.util.UUID;

public interface CardReadService {

    MyActiveCardResponse getMyActiveCard(UUID userId);

    PaginatedCardListResponse getNearbyCards(double lat, double lng, String cursor, UUID userId);

    CardDetailResponse getCard(UUID cardId);

    List<CardApplySummaryResponse> getApplies(UUID cardId, UUID userId);
}
