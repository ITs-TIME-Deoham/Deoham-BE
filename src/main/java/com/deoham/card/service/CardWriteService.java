package com.deoham.card.service;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardDetailResponse;

import java.util.UUID;

public interface CardWriteService {

    CardDetailResponse createCard(CreateCardRequest request, UUID userId);

    void cancelCard(UUID cardId, UUID userId);

    void completeCard(UUID cardId, UUID userId);

    void retryCard(UUID cardId, UUID userId);
}
