package com.deoham.card.service;

import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.dto.response.PaginatedCardListResponse;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultCardReadService implements CardReadService {

    private static final int PAGE_SIZE = 20;
    private final CardRepository cardRepository;
    private final CardApplyRepository cardApplyRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<CardDetailResponse> getMyActiveCard(UUID userId) {
        return cardRepository.findFirstByRequesterIdAndStatusIn(userId, List.of(CardStatus.OPEN, CardStatus.MATCHED))
                .map(this::toDetailResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedCardListResponse getNearbyCards(double lat, double lng, String cursor) {
        CursorData cursorData = parseCursor(cursor);
        List<Object[]> rows = cardRepository.findNearbyCards(lat, lng, cursorData.distance(), cursorData.cardId());

        boolean hasMore = rows.size() > PAGE_SIZE;
        List<CardDetailResponse> cards = rows.stream()
                .limit(PAGE_SIZE)
                .map(row -> new CardDetailResponse(
                        (UUID) row[0],
                        (UUID) row[1],
                        (String) row[2],
                        CardCategory.valueOf((String) row[3]),
                        (String) row[4],
                        toInstant(row[5]),
                        CardStatus.valueOf((String) row[6]),
                        row[7] != null ? PreferredGender.valueOf((String) row[7]) : null,
                        (Integer) row[8],
                        (Integer) row[9],
                        (Integer) row[10],
                        toInstant(row[11]),
                        toInstant(row[12]),
                        ((Number) row[13]).doubleValue()
                ))
                .toList();

        String nextCursor = hasMore ? encodeCursor(((Number) rows.get(PAGE_SIZE - 1)[13]).doubleValue(), rows.get(PAGE_SIZE - 1)[0].toString()) : null;
        return new PaginatedCardListResponse(cards, nextCursor);
    }

    @Override
    @Transactional(readOnly = true)
    public CardDetailResponse getCard(UUID cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        return toDetailResponse(card);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CardApplySummaryResponse> getApplies(UUID cardId, UUID userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        if (!card.getRequester().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "카드 작성자만 신청 목록을 조회할 수 있습니다.");
        }

        return cardApplyRepository.findByCard(card).stream()
                .map(apply -> new CardApplySummaryResponse(
                        apply.getId(),
                        apply.getApplicant().getId(),
                        apply.getApplicant().getNickname(),
                        apply.getApplicant().getProfileImageUrl(),
                        apply.getStatus(),
                        apply.getAppliedAt()
                ))
                .toList();
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
                card.getUpdatedAt(),
                null  // distanceMeters: null for non-nearby cards (only populated in getNearbyCards)
        );
    }

    private static Instant toInstant(Object value) {
        if (value == null) return null;
        if (value instanceof Instant inst) return inst;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant();
        if (value instanceof java.time.OffsetDateTime odt) return odt.toInstant();
        if (value instanceof java.time.ZonedDateTime zdt) return zdt.toInstant();
        if (value instanceof java.time.LocalDateTime ldt) return ldt.atZone(java.time.ZoneOffset.UTC).toInstant();
        if (value instanceof java.sql.Date sqlDate) return sqlDate.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        if (value instanceof java.util.Date date) return date.toInstant();
        throw new IllegalArgumentException("Unsupported timestamp type: " + value.getClass().getName());
    }

    private String encodeCursor(double distance, String cardId) {
        String cursorData = distance + "|" + cardId;
        return Base64.getEncoder().encodeToString(cursorData.getBytes(StandardCharsets.UTF_8));
    }

    private CursorData parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return new CursorData(null, null);
        }
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|");
            if (parts.length != 2) {
                return new CursorData(null, null);
            }
            double distance = Double.parseDouble(parts[0]);
            return new CursorData(distance, parts[1]);
        } catch (Exception e) {
            return new CursorData(null, null);
        }
    }

    private record CursorData(Double distance, String cardId) {
    }
}
