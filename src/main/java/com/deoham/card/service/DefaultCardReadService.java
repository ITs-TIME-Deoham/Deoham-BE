package com.deoham.card.service;

import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.CardSummaryResponse;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultCardReadService implements CardReadService {

    private final CardRepository cardRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CardDetailResponse> getMyActiveCard(UUID userId) {
        return cardRepository.findFirstByRequesterIdAndStatusIn(userId, List.of(CardStatus.OPEN, CardStatus.MATCHED))
                .map(this::toDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardSummaryResponse> getNearbyCards(double lat, double lng, double radiusMeters) {
        return cardRepository.findNearbyCards(lat, lng, radiusMeters).stream()
                .map(row -> new CardSummaryResponse(
                        (UUID) row[0],
                        CardCategory.valueOf((String) row[1]),
                        CardStatus.valueOf((String) row[2]),
                        toInstant(row[3]),
                        row[4] != null ? PreferredGender.valueOf((String) row[4]) : null,
                        (Integer) row[5],
                        (Integer) row[6],
                        ((Number) row[7]).doubleValue(),
                        toInstant(row[8])
                ))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CardDetailResponse getCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        return toDetailResponse(card);
    }

    private CardDetailResponse toDetailResponse(Card card) {
        return new CardDetailResponse(
                card.getId(),
                card.getRequester().getId(),
                card.getRequester().getProfileImageUrl(),
                card.getCategory(),
                card.getDescription(),
                card.getExpiresAt(),
                card.getStatus(),
                card.getPreferredGender(),
                card.getPreferredAgeMin(),
                card.getPreferredAgeMax(),
                card.getRetryCount(),
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    private static Instant toInstant(Object value) {
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        return (Instant) value;
    }
}
