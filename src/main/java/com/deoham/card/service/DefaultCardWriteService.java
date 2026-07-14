package com.deoham.card.service;

import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.dto.response.CardApplySummaryResponse;
import com.deoham.card.dto.response.CardDetailResponse;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardApply;
import com.deoham.card.entity.CardApplyStatus;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.repository.CardApplyRepository;
import com.deoham.card.repository.CardRepository;
import com.deoham.global.exception.BusinessException;
import com.deoham.global.exception.ErrorCode;
import com.deoham.global.metrics.MetricsRegistry;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserWriteService;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DefaultCardWriteService implements CardWriteService {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    private final CardRepository cardRepository;
    private final CardApplyRepository cardApplyRepository;
    private final UserRepository userRepository;
    private final UserWriteService userWriteService;
    private final MetricsRegistry metricsRegistry;

    @Override
    @Transactional
    public CardDetailResponse createCard(CreateCardRequest request, UUID userId) {
        Timer.Sample sample = metricsRegistry.startCardCreateTimer();
        try {
            User requester = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

            Point location = GEOMETRY_FACTORY.createPoint(new Coordinate(request.longitude(), request.latitude()));

            Card card = Card.builder()
                    .requester(requester)
                    .category(request.category())
                    .description(request.description())
                    .location(location)
                    .expiresAt(Instant.now().plus(Card.EXPIRY_DURATION))
                    .radiusM(11100000)
                    .city("korea")
                    .preferredGender(request.preferredGender())
                    .preferredAgeMin(request.preferredAgeMin())
                    .preferredAgeMax(request.preferredAgeMax())
                    .build();

            cardRepository.save(card);
            requester.incrementHelpRequestCount();
            requester.markCardCreated();

            CardDetailResponse response = new CardDetailResponse(
                    card.getId(),
                    card.getRequester().getId(),
                    card.getRequester().getNickname(),
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
                    null  // distanceMeters: not applicable for created cards
            );
            metricsRegistry.recordCardCreateSuccess(sample);
            return response;
        } catch (Exception exception) {
            metricsRegistry.recordCardCreateFailure(sample, exception);
            throw exception;
        }
    }

    @Override
    @Transactional
    public void cancelCard(UUID cardId, UUID userId) {
        Card card = findCardAndValidateOwner(cardId, userId);
        if (card.getStatus() != CardStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "OPEN 상태의 카드만 취소할 수 있습니다.");
        }
        card.updateStatus(CardStatus.CANCELLED);
    }

    @Override
    @Transactional
    public void completeCard(UUID cardId, UUID userId) {
        Card card = findCardAndValidateOwner(cardId, userId);
        if (card.getStatus() != CardStatus.MATCHED) {
            throw new BusinessException(ErrorCode.CONFLICT, "MATCHED 상태의 카드만 완료할 수 있습니다.");
        }
        CardApply acceptedApply = cardApplyRepository.findByCardAndStatus(card, CardApplyStatus.ACCEPTED)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "승인된 신청을 찾을 수 없습니다."));

        card.updateStatus(CardStatus.COMPLETED);

        userWriteService.updateHelpCounts(card.getRequester().getId(), acceptedApply.getApplicant().getId());
    }

    @Override
    @Transactional
    public void retryCard(UUID cardId, UUID userId) {
        Card card = findCardAndValidateOwner(cardId, userId);
        if (card.getStatus() != CardStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "OPEN 상태의 카드만 재요청할 수 있습니다.");
        }
        if (card.getRetryCount() >= Card.MAX_RETRY_COUNT) {
            throw new BusinessException(ErrorCode.CONFLICT, "재요청 횟수를 초과했습니다. (최대 " + Card.MAX_RETRY_COUNT + "회)");
        }
        Instant retriedAt = Instant.now();
        card.incrementRetryCount();
        card.updateExpiresAt(retriedAt.plus(Card.EXPIRY_DURATION));
    }

    @Override
    @Transactional
    public CardApplySummaryResponse submitApply(UUID cardId, UUID applicantId) {
        Card card = cardRepository.findByIdWithRequester(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        if (card.getStatus() != CardStatus.OPEN) {
            throw new BusinessException(ErrorCode.CONFLICT, "OPEN 상태의 카드에만 신청할 수 있습니다.");
        }

        User applicant = userRepository.findById(applicantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        if (card.getRequester().getId().equals(applicantId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "본인 카드에는 신청할 수 없습니다.");
        }

        if (cardApplyRepository.existsByCardAndApplicant(card, applicant)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 신청한 카드입니다.");
        }

        CardApply apply = CardApply.builder()
                .card(card)
                .applicant(applicant)
                .build();

        cardApplyRepository.save(apply);

        apply.accept();
        card.updateStatus(CardStatus.MATCHED);

        return new CardApplySummaryResponse(
                apply.getId(),
                apply.getApplicant().getId(),
                apply.getApplicant().getNickname(),
                apply.getApplicant().getProfileImageUrl(),
                apply.getStatus(),
                apply.getAppliedAt()
        );
    }

    @Override
    @Transactional
    public void cancelApply(UUID cardId, UUID userId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));

        User applicant = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "사용자를 찾을 수 없습니다."));

        CardApply apply = cardApplyRepository.findByCardAndApplicant(card, applicant)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "신청을 찾을 수 없습니다."));

        if (apply.getStatus() != CardApplyStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "PENDING 상태의 신청만 취소할 수 있습니다.");
        }

        cardApplyRepository.delete(apply);
    }

    @Override
    @Transactional
    public void expireCards() {
        List<Card> expiredCards = cardRepository.findByStatusInAndExpiresAtBefore(
                List.of(CardStatus.OPEN, CardStatus.MATCHED),
                Instant.now()
        );
        expiredCards.forEach(card -> card.updateStatus(CardStatus.CANCELLED));
    }

    private Card findCardAndValidateOwner(UUID cardId, UUID userId) {
        Card card = cardRepository.findByIdWithRequester(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "카드를 찾을 수 없습니다."));
        if (!card.getRequester().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "카드 작성자만 수행할 수 있습니다.");
        }
        return card;
    }
}
