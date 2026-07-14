package com.deoham.card.service;

import com.deoham.TestcontainersConfiguration;
import com.deoham.card.dto.request.CreateCardRequest;
import com.deoham.card.entity.Card;
import com.deoham.card.entity.CardCategory;
import com.deoham.card.entity.CardStatus;
import com.deoham.card.entity.PreferredGender;
import com.deoham.card.repository.CardRepository;
import com.deoham.user.entity.User;
import com.deoham.user.repository.UserRepository;
import com.deoham.user.service.UserWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
@Testcontainers(disabledWithoutDocker = true)
@DisplayName("Card 만료 기능 테스트")
class CardExpirationTest {

    @Autowired
    private CardWriteService cardWriteService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    private User user;
    private UUID userId;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .firebaseUid("firebase_test_" + UUID.randomUUID())
                .nickname("test_user")
                .build();
        user = userRepository.save(user);
        userId = user.getId();
    }

    @Test
    @DisplayName("카드 생성 후 30분이 경과하면 CANCELLED 상태로 변경된다")
    void cardIsExpiredAfter30Minutes() {
        // Given
        CreateCardRequest request = new CreateCardRequest(
                CardCategory.MEAL,
                "test description",
                37.5,
                127.0,
                PreferredGender.ANY,
                20,
                40
        );

        var cardDetail = cardWriteService.createCard(request, userId);
        UUID cardId = cardDetail.id();

        Card card = cardRepository.findById(cardId).orElseThrow();
        assertThat(card.getStatus()).isEqualTo(CardStatus.OPEN);
        assertThat(card.getExpiresAt()).isBetween(
                Instant.now().plus(Card.EXPIRY_DURATION).minusSeconds(1),
                Instant.now().plus(Card.EXPIRY_DURATION).plusSeconds(1)
        );

        // When: expiresAt을 현재 시간으로 설정하여 만료 상태 시뮬레이션
        card.updateExpiresAt(Instant.now().minusSeconds(1));
        cardRepository.save(card);

        // cardWriteService.expireCards() 호출
        cardWriteService.expireCards();

        // Then
        Card expiredCard = cardRepository.findById(cardId).orElseThrow();
        assertThat(expiredCard.getStatus()).isEqualTo(CardStatus.CANCELLED);
    }

    @Test
    @DisplayName("retry 후 만료 시간이 30분으로 재설정된다")
    void expirationTimeResetAfterRetry() {
        // Given
        CreateCardRequest request = new CreateCardRequest(
                CardCategory.MEAL,
                "test description",
                37.5,
                127.0,
                PreferredGender.ANY,
                20,
                40
        );

        var cardDetail = cardWriteService.createCard(request, userId);
        UUID cardId = cardDetail.id();

        Card card = cardRepository.findById(cardId).orElseThrow();
        Instant originalExpiresAt = card.getExpiresAt();

        // When: retry 실행
        cardWriteService.retryCard(cardId, userId);

        // Then
        Card retriedCard = cardRepository.findById(cardId).orElseThrow();
        assertThat(retriedCard.getRetryCount()).isEqualTo(1);
        assertThat(retriedCard.getExpiresAt()).isAfter(originalExpiresAt);
        assertThat(retriedCard.getExpiresAt()).isBetween(
                Instant.now().plus(Card.EXPIRY_DURATION).minusSeconds(1),
                Instant.now().plus(Card.EXPIRY_DURATION).plusSeconds(1)
        );
    }

    @Test
    @DisplayName("retry 후 30분이 경과하면 다시 CANCELLED 상태로 변경된다")
    void cardIsExpiredAfterRetryExpiration() {
        // Given
        CreateCardRequest request = new CreateCardRequest(
                CardCategory.MEAL,
                "test description",
                37.5,
                127.0,
                PreferredGender.ANY,
                20,
                40
        );

        var cardDetail = cardWriteService.createCard(request, userId);
        UUID cardId = cardDetail.id();

        // When: retry 실행
        cardWriteService.retryCard(cardId, userId);

        Card retriedCard = cardRepository.findById(cardId).orElseThrow();
        assertThat(retriedCard.getStatus()).isEqualTo(CardStatus.OPEN);
        assertThat(retriedCard.getRetryCount()).isEqualTo(1);

        // expiresAt을 현재 시간으로 설정하여 만료 상태 시뮬레이션
        retriedCard.updateExpiresAt(Instant.now().minusSeconds(1));
        cardRepository.save(retriedCard);

        // expireCards() 호출
        cardWriteService.expireCards();

        // Then
        Card expiredCard = cardRepository.findById(cardId).orElseThrow();
        assertThat(expiredCard.getStatus()).isEqualTo(CardStatus.CANCELLED);
        assertThat(expiredCard.getRetryCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최대 3회까지 retry 가능하고, 3회 이후에는 에러 발생")
    void maxRetryCountIs3() {
        // Given
        CreateCardRequest request = new CreateCardRequest(
                CardCategory.MEAL,
                "test description",
                37.5,
                127.0,
                PreferredGender.ANY,
                20,
                40
        );

        var cardDetail = cardWriteService.createCard(request, userId);
        UUID cardId = cardDetail.id();

        // When & Then
        cardWriteService.retryCard(cardId, userId);
        assertThat(cardRepository.findById(cardId).orElseThrow().getRetryCount()).isEqualTo(1);

        cardWriteService.retryCard(cardId, userId);
        assertThat(cardRepository.findById(cardId).orElseThrow().getRetryCount()).isEqualTo(2);

        cardWriteService.retryCard(cardId, userId);
        assertThat(cardRepository.findById(cardId).orElseThrow().getRetryCount()).isEqualTo(3);

        // 4번째 retry는 실패해야 함
        var finalCard = cardRepository.findById(cardId).orElseThrow();
        var exception = org.junit.jupiter.api.Assertions.assertThrows(
                com.deoham.global.exception.BusinessException.class,
                () -> cardWriteService.retryCard(cardId, userId)
        );
        assertThat(exception.getMessage()).contains("재요청 횟수를 초과했습니다");
    }

    @Test
    @DisplayName("expireCards()는 OPEN 상태의 만료된 카드만 CANCELLED로 변경한다")
    void expireCardsOnlyAffectsOpenCards() {
        // Given
        CreateCardRequest request = new CreateCardRequest(
                CardCategory.MEAL,
                "test description",
                37.5,
                127.0,
                PreferredGender.ANY,
                20,
                40
        );

        var cardDetail = cardWriteService.createCard(request, userId);
        UUID cardId = cardDetail.id();

        Card card = cardRepository.findById(cardId).orElseThrow();
        // 카드를 CANCELLED로 수동 설정
        card.updateStatus(CardStatus.CANCELLED);
        card.updateExpiresAt(Instant.now().minusSeconds(1));
        cardRepository.save(card);

        // When
        cardWriteService.expireCards();

        // Then: 상태가 유지되어야 함
        Card unchangedCard = cardRepository.findById(cardId).orElseThrow();
        assertThat(unchangedCard.getStatus()).isEqualTo(CardStatus.CANCELLED);
    }
}
